package gg.tater.core

import gg.tater.core.controllers.LimboController
import gg.tater.core.controllers.ServerStatusController
import gg.tater.core.controllers.SpawnController
import gg.tater.core.controllers.island.IslandController
import gg.tater.core.controllers.leaderboard.LeaderboardController
import gg.tater.core.controllers.player.PlayerController
import gg.tater.core.controllers.player.combat.CombatController
import gg.tater.core.controllers.player.duel.DuelController
import gg.tater.core.controllers.player.warp.WarpController
import gg.tater.shared.network.Agones
import gg.tater.shared.redis.Redis
import io.github.cdimascio.dotenv.Dotenv
import me.lucko.helper.Services
import me.lucko.helper.plugin.ExtendedJavaPlugin
import okhttp3.OkHttpClient
import org.bukkit.Bukkit

class CorePlugin : ExtendedJavaPlugin() {

    override fun enable() {
        val client = OkHttpClient()
        Services.provide(OkHttpClient::class.java, client)
        val actions = Agones(client)

        val id = actions.getGameServerId()
        if (id == null) {
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
        bindModule(ServerStatusController(id, actions, redis))
        bind(AutoCloseable {
            redis.servers().remove(id)
        })

        if (id.contains("limbo")) {
            bindModule(LimboController(redis))
            return
        }

        if (id.contains("duel")) {
            bindModule(DuelController(redis, credential))
            return
        }

        val islands = bindModule(IslandController(redis, id, credential))

        bindModule(CombatController(redis))
        bindModule(SpawnController(redis, id))
        bindModule(PlayerController(this, redis, id, islands))
        bindModule(LeaderboardController())
        bindModule(WarpController(redis, id))
    }
}