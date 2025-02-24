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
import gg.tater.proxy.listener.IslandPlacementListener
import gg.tater.proxy.listener.PlayerRedirectListener
import gg.tater.proxy.tasks.ServerRegistryTask
import gg.tater.shared.hexToBytes
import gg.tater.shared.network.Agones
import gg.tater.shared.network.ProxyDataModel
import gg.tater.shared.network.server.ServerType
import gg.tater.shared.redis.Redis
import io.github.cdimascio.dotenv.Dotenv
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import okhttp3.*
import org.slf4j.Logger
import java.io.IOException
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Plugin(id = "velocity", version = "1.0")
class ProxyPlugin @Inject constructor(
    private val proxy: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dir: Path
) {

    private companion object {
        const val TEXTURE_PACK_HEX_REQUEST_URL = "https://database.oneblock.is/hash/{key}"

        val ERROR_FETCHING_PACK_MESSAGE = Component.text(
            "There was an error fetching your texture pack, " +
                    "please contact support if this issue persists.", NamedTextColor.RED
        )
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

        IslandPlacementListener(proxy, redis)
        PlayerRedirectListener(proxy, redis)
        ServerRegistryTask(dir, env, proxy, redis, actions, data, logger)
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
                player.disconnect(ERROR_FETCHING_PACK_MESSAGE)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    player.disconnect(ERROR_FETCHING_PACK_MESSAGE)
                    return
                }

                val body = response.body ?: return
                val texturePackHash = hexToBytes(body.string())

                //cert.pem  chain.pem  fullchain.pem  privkey.pem

                proxy.scheduler.buildTask(this, Runnable {
                    // Player already has pack applied
                    if (player.appliedResourcePacks.any { it.hash.contentEquals(texturePackHash) }) {
                        logger.info("Player has most recent resource pack loaded, ignoring.")
                        return@Runnable
                    }

                    player.sendResourcePackOffer(
                        proxy.createResourcePackBuilder("https://tp.oneblock.is/pack/${player.uniqueId}/${textureApiKey}")
                            .setId(player.uniqueId)
                            .setHash(texturePackHash)
                            .setShouldForce(true)
                            .setPrompt(Component.text("Accept texture pack to play", NamedTextColor.GREEN))
                            .build()
                    )
                }).delay(1.seconds.toJavaDuration()).schedule()
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