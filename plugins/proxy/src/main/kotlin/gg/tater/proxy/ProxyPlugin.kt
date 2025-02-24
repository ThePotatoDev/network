package gg.tater.proxy

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.tater.proxy.listener.IslandPlacementListener
import gg.tater.proxy.listener.PlayerRedirectListener
import gg.tater.shared.hexToBytes
import gg.tater.shared.network.Agones
import gg.tater.shared.network.ProxyDataModel
import gg.tater.shared.network.server.ServerDataModel
import gg.tater.shared.network.server.ServerType
import gg.tater.shared.redis.Redis
import io.github.cdimascio.dotenv.Dotenv
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CustomObjectsApi
import io.kubernetes.client.util.Config
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import okhttp3.*
import org.slf4j.Logger
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@Plugin(id = "velocity", version = "1.0")
class ProxyPlugin @Inject constructor(
    private val proxy: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dir: Path
) {

    private val removals: MutableSet<String> = Collections.newSetFromMap(
        ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.CREATED)
            .expiration(5L, TimeUnit.SECONDS)
            .build()
    )

    private companion object {
        val ERROR_FETCHING_PACK_MESSAGE = Component.text(
            "There was an error fetching your texture pack, " +
                    "please contact support if this issue persists.", NamedTextColor.RED
        )

        const val TEXTURE_PACK_HEX_REQUEST_URL = "https://tp.oneblock.is/request/hash/{key}"
        const val TEXTURE_PACK_DATA_REQUEST_URL = "https://tp.oneblock.is/request/pack/{playerId}/{key}"
        const val GROUP = "agones.dev"
        const val VERSION = "v1"
        const val NAMESPACE = "default"
        const val PLURAL = "gameservers"
    }

    private lateinit var redis: Redis

    private val http = OkHttpClient()
    private val env = Dotenv.load()
    private val textureApiKey = env.get("TEXTURE_PACK_SERVICE_API_KEY")

    @Subscribe
    private fun onProxyInit(event: ProxyInitializeEvent) {
        val actions = Agones(http)
        val data = ProxyDataModel()

        val credential = Redis.Credential(
            env.get("REDIS_USERNAME"),
            env.get("REDIS_PASSWORD"),
            env.get("REDIS_ADDRESS"),
            env.get("REDIS_PORT").toInt()
        )

        this.redis = Redis(credential).apply {
            servers().clear() //TODO() This is a debug, REMOVE before prod (Need a better way to remove old servers)
        }

        val client: ApiClient =
            Config.fromConfig(if (env.get("ENV").equals("dev")) "$dir/config_dev" else "$dir/config_local")
        Configuration.setDefaultApiClient(client)
        val api = CustomObjectsApi()

        proxy.scheduler.buildTask(this, Runnable {
            actions.health()

            data.players = proxy.playerCount
            redis.proxy().set(data)

            // Agones status handling
            if (proxy.playerCount > 0) {
                actions.allocate()
            } else {
                actions.ready()
            }

            // K8's api to find active nodes & register them to the proxy
            // List all game servers in the default namespace
            val servers = api.listNamespacedCustomObject(
                GROUP,
                VERSION,
                NAMESPACE,
                PLURAL,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ) as Map<*, *>

            // Extract and print the game server details
            val items = servers["items"] as List<Map<*, *>>
            for (item in items) {
                val metadata = item["metadata"] as Map<*, *>
                val status = item["status"] as Map<*, *>
                val address = status["address"] as String?
                val ports = status["ports"] as List<Map<*, *>>? ?: continue
                val state = status["state"] as String? ?: continue
                val name = metadata["name"] as String? ?: continue

                // Do not register proxy servers
                if (name.contains("proxy")) continue

                if (state == "Ready" && proxy.getServer(name).isEmpty && !removals.contains(name)) {
                    val info = ServerInfo(
                        name, InetSocketAddress(
                            address, ports.firstOrNull()?.get("port")
                                .toString()
                                .split(".")[0]
                                .toInt()
                        )
                    )

                    proxy.registerServer(info)
                    logger.info("Registered $name")

                    val type = ServerType.valueOf(name.split("-")[0].uppercase())
                    redis.servers().putIfAbsentAsync(name, ServerDataModel(name, type))
                    return@Runnable
                }
            }

            for (server in proxy.allServers) {
                server.ping().whenComplete { _, throwable ->
                    // Server is alive
                    if (throwable == null) {
                        return@whenComplete
                    }

                    val info = server.serverInfo
                    val name = info.name

                    proxy.unregisterServer(info)
                    removals.add(name)
                    redis.servers().removeAsync(name)

                    logger.info("Unregistered $name")
                }
            }
        }).delay(Duration.ofSeconds(2L))
            .repeat(Duration.ofSeconds(2L))
            .schedule()

        IslandPlacementListener(proxy, redis).exec()
        PlayerRedirectListener(proxy, redis).exec()
    }

    @Subscribe
    private fun onLogin(event: LoginEvent) {
        val player = event.player

        http.newCall(
            Request.Builder()
                .url(TEXTURE_PACK_HEX_REQUEST_URL.replace("{key}", textureApiKey))
                .build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                player.disconnect(ERROR_FETCHING_PACK_MESSAGE)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body ?: return
                val texturePackHash = hexToBytes(body.string())

                // Player already has pack applied
                if (player.appliedResourcePacks.any { it.hash.contentEquals(texturePackHash) }) {
                    logger.info("Player has most recent resource pack loaded, ignoring.")
                    return
                }

                player.sendResourcePackOffer(
                    proxy.createResourcePackBuilder(
                        TEXTURE_PACK_DATA_REQUEST_URL
                            .replace("{playerId}", player.uniqueId.toString())
                            .replace("{key}", textureApiKey)
                    )
                        .setId(player.uniqueId)
                        .setHash(texturePackHash)
                        .setShouldForce(true)
                        .setPrompt(Component.text("Accept texture pack to play", NamedTextColor.GREEN))
                        .build()
                )
            }
        })
    }

    @Subscribe
    private fun onResourcePackOffer(event: PlayerResourcePackStatusEvent) {
        val status = event.status
        if (status != PlayerResourcePackStatusEvent.Status.ACCEPTED
            && status != PlayerResourcePackStatusEvent.Status.DOWNLOADED
            && status != PlayerResourcePackStatusEvent.Status.SUCCESSFUL
        ) {
            event.player.disconnect(
                Component.text(
                    "You failed to download the resource pack. Please rejoin if this was an accident.",
                    NamedTextColor.RED
                )
            )
        }
    }

    @Subscribe
    private fun onPlayerChooseServer(event: PlayerChooseInitialServerEvent) {
        val server = redis.getReadyServer(ServerType.SPAWN)
        val info = proxy.getServer(server.id).orElse(null)
        event.setInitialServer(info)
    }
}