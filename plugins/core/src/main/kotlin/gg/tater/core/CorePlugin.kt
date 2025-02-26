package gg.tater.core

import gg.tater.shared.network.Agones
import gg.tater.shared.player.BasePlayerController
import gg.tater.shared.player.pm.PlayerPrivateMessageController
import gg.tater.shared.plugin.GameServerPlugin
import gg.tater.shared.redis.Redis
import gg.tater.shared.server.ServerDataService
import gg.tater.shared.server.ServerStatusController
import io.github.cdimascio.dotenv.Dotenv
import me.lucko.helper.Services
import okhttp3.OkHttpClient
import org.bukkit.Bukkit

class CorePlugin : GameServerPlugin(), ServerDataService {

    private lateinit var serverId: String

    override fun enable() {
        Services.provide(CorePlugin::class.java, this)
        val actions =
            Services.provide(Agones::class.java, Agones(Services.provide(OkHttpClient::class.java, OkHttpClient())))

        val server = actions.getGameServerId()
        if (server == null) {
            logger.severe("Failed to get game server id")
            Bukkit.shutdown()
            return
        }

        this.serverId = server
        Services.provide(ServerDataService::class.java, this)

        val env = Dotenv.load()

        Services.provide(
            Redis::class.java, Redis(
                Services.provide(
                    Redis.Credential::class.java, Redis.Credential(
                        env.get("REDIS_USERNAME"),
                        env.get("REDIS_PASSWORD"),
                        env.get("REDIS_ADDRESS"),
                        env.get("REDIS_PORT").toInt()
                    )
                )
            )
        )

        useController(null, BasePlayerController::class)
        useController(null, PlayerPrivateMessageController::class)
        useController(null, ServerStatusController::class)
    }

    override fun id(): String {
        return serverId
    }
}