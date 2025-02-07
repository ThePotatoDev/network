package gg.tater.shared.leaderboard

import me.lucko.helper.terminable.module.TerminableModule

interface LeaderboardService : TerminableModule {

    fun addLeaderboard(leaderboard: Leaderboard): Boolean

    fun removeLeaderboard(leaderboard: Leaderboard): Boolean

    fun getLeaderboard(id: String): Leaderboard?

}