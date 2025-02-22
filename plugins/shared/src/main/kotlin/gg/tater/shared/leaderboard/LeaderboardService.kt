package gg.tater.shared.leaderboard

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.time.Instant
import java.util.concurrent.CompletionStage

interface LeaderboardService : TerminableModule {

    fun getLastRefresh(leaderboard: Leaderboard<*, *>): CompletionStage<Instant>

    fun setLastRefresh(leaderboard: Leaderboard<*, *>): RFuture<Long>

}