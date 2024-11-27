package gg.tater.proxy

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.tater.shared.redis.Redis
import gg.tater.shared.island.message.placement.IslandPlacementResponse
import gg.tater.shared.network.Agones
import gg.tater.shared.network.model.ServerDataModel
import gg.tater.shared.network.model.ServerType
import gg.tater.shared.player.PlayerRedirectRequest
import io.github.cdimascio.dotenv.Dotenv
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CustomObjectsApi
import io.kubernetes.client.util.Config
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import okhttp3.OkHttpClient
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@Plugin(id = "velocity", version = "1.0")
class ProxyPlugin @Inject constructor(
    private val network: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dir: Path
) {

    companion object {
        const val GROUP = "agones.dev"
        const val VERSION = "v1"
        const val NAMESPACE = "default"
        const val PLURAL = "gameservers"
    }

    private lateinit var redis: Redis

    private val removals: MutableSet<String> = Collections.newSetFromMap(
        ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.CREATED)
            .expiration(5L, TimeUnit.SECONDS)
            .build()
    )

    @Subscribe
    private fun onProxyInit(event: ProxyInitializeEvent) {
        val actions = Agones(OkHttpClient())

        val env = Dotenv.load()
        val credential = Redis.Credential(
            env.get("REDIS_USERNAME"),
            env.get("REDIS_PASSWORD"),
            env.get("REDIS_ADDRESS"),
            env.get("REDIS_PORT").toInt()
        )

        this.redis = Redis(credential)
        redis.servers().clear()

        val client: ApiClient = Config.fromConfig("$dir/config")
        Configuration.setDefaultApiClient(client)
        val api = CustomObjectsApi()

        redis.listen<PlayerRedirectRequest> {
            val player = network.getPlayer(it.uuid).orElse(null) ?: return@listen
            val target: RegisteredServer?

            // If the direct server exists, move them there instead
            val serverId = it.server
            if (serverId != null) {
                target = network.getServer(serverId).orElse(null)
            } else {
                val id = redis.getReadyServer(it.type).id
                target = network.getServer(id).orElse(null)
            }

            if (target == null) return@listen
            player.createConnectionRequest(target).fireAndForget()
        }

        // When their island is ready, redirect them to the server
        redis.listen<IslandPlacementResponse> {
            val player = network.getPlayer(it.playerId).orElse(null) ?: return@listen
            val server = network.getServer(it.server).orElse(null) ?: return@listen
            if (it.internal) {
                player.createConnectionRequest(server).fireAndForget()
            }
        }

        network.scheduler.buildTask(this, Runnable {
            actions.health()

            // Agones status handling
            if (network.playerCount > 0) {
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

                if (state == "Ready" && network.getServer(name).isEmpty && !removals.contains(name)) {
                    val info = ServerInfo(
                        name, InetSocketAddress(
                            address, ports.firstOrNull()?.get("port")
                                .toString()
                                .split(".")[0]
                                .toInt()
                        )
                    )

                    network.registerServer(info)
                    logger.info("Registered $name")

                    val type = ServerType.valueOf(name.split("-")[0].uppercase())
                    redis.servers().putIfAbsentAsync(name, ServerDataModel(name, type))
                    return@Runnable
                }
            }

            for (server in network.allServers) {
                server.ping().whenComplete { _, throwable ->
                    // Server is alive
                    if (throwable == null) {
                        return@whenComplete
                    }

                    val info = server.serverInfo
                    val name = info.name

                    network.unregisterServer(info)
                    removals.add(name)
                    redis.servers().removeAsync(name)

                    logger.info("Unregistered $name")
                }
            }
        }).repeat(Duration.ofSeconds(1L)).schedule()
    }

    @Subscribe
    private fun onServerConnect(event: PlayerChooseInitialServerEvent) {
        val server = redis.getReadyServer(ServerType.LIMBO)
        val info = network.getServer(server.id).orElse(null)
        event.setInitialServer(info)
    }
}