package gg.tater.core.controllers.leaderboard

import gg.tater.shared.leaderboard.Leaderboard
import gg.tater.shared.leaderboard.LeaderboardService
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer

class LeaderboardController : LeaderboardService {

    override fun setup(consmer: TerminableConsumer) {
        Services.provide(LeaderboardService::class.java, this)
    }

    override fun addLeaderboard(leaderboard: Leaderboard): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeLeaderboard(leaderboard: Leaderboard): Boolean {
        TODO("Not yet implemented")
    }

    override fun getLeaderboard(id: String): Leaderboard? {
        TODO("Not yet implemented")
    }
}