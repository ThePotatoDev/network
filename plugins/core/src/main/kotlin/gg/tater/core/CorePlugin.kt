package gg.tater.core

import gg.tater.core.controllers.island.IslandController
import gg.tater.core.controllers.leaderboard.LeaderboardController
import gg.tater.core.controllers.player.PlayerController
import gg.tater.core.controllers.player.combat.CombatController
import gg.tater.core.controllers.player.duel.DuelController
import gg.tater.core.controllers.player.teleport.TeleportController
import gg.tater.core.controllers.player.warp.WarpController
import gg.tater.core.controllers.server.ServerStatusController
import gg.tater.core.controllers.server.SpawnController
import gg.tater.shared.network.Agones
import gg.tater.shared.redis.Redis
import io.github.cdimascio.dotenv.Dotenv
import me.lucko.helper.Helper
import me.lucko.helper.Services
import me.lucko.helper.plugin.ExtendedJavaPlugin
import okhttp3.OkHttpClient
import org.bukkit.Bukkit

class CorePlugin : ExtendedJavaPlugin() {

    override fun enable() {
        val client = OkHttpClient()
        Services.provide(OkHttpClient::class.java, client)
        val actions = Agones(client)

        val server = actions.getGameServerId()
        if (server == null) {
            logger.severe("Failed to get game server id")
            Bukkit.shutdown()
            return
        }

        val env = Dotenv.load()
        val credential = Redis.Credential(
            env.get("REDIS_USERNAME"),
            env.get("REDIS_PASSWORD"),
            env.get("REDIS_ADDRESS"),
            env.get("REDIS_PORT").toInt()
        )

        val redis = Services.provide(Redis::class.java, Redis(credential))
        bindModule(ServerStatusController(server, actions, redis))
        bind(AutoCloseable {
            redis.servers().remove(server)
        })

        if (server.contains("duel")) {
            bindModule(DuelController(redis, credential))
            return
        }

        val islands = bindModule(IslandController(redis, server, credential))

        if (Helper.plugins().isPluginEnabled("FancyNpcs")) {
//            bindModule(CombatController(redis))
        }

//        bindModule(TeleportController(redis, server))
        bindModule(SpawnController(redis, server))
        bindModule(PlayerController(this, redis, server, islands))
        bindModule(LeaderboardController())
        bindModule(WarpController(redis, server))
    }
}