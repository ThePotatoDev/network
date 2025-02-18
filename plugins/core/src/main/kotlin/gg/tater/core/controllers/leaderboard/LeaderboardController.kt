package gg.tater.core.controllers.leaderboard

import gg.tater.shared.Controller
import gg.tater.shared.leaderboard.Leaderboard
import gg.tater.shared.leaderboard.LeaderboardService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.redisson.api.RFuture
import java.time.Instant
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

@Controller(
    id = "leaderboard-controller"
)
class LeaderboardController : LeaderboardService {

    private companion object {
        const val LEADERBOARD_REFRESH_MAP_NAME = "leaderboard_refresh"
    }

    private val redis = Services.load(Redis::class.java)

    override fun setup(consmer: TerminableConsumer) {
        Services.provide(LeaderboardService::class.java, this)

        Schedulers.async().runRepeating(Runnable {

        }, 1L, TimeUnit.SECONDS, 1L, TimeUnit.HOURS)
    }

    override fun getLastRefresh(leaderboard: Leaderboard<*, *>): CompletionStage<Instant> {
        return redis.client.getMap<String, Long>(LEADERBOARD_REFRESH_MAP_NAME)
            .getAsync(leaderboard.id()).thenApply { Instant.ofEpochMilli(it) }
    }

    override fun setLastRefresh(leaderboard: Leaderboard<*, *>): RFuture<Long> {
        return redis.client.getMap<String, Long>(LEADERBOARD_REFRESH_MAP_NAME)
            .putAsync(leaderboard.id(), Instant.now().toEpochMilli())
    }
}