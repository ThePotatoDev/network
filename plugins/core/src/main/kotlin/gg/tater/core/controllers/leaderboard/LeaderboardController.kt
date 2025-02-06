package gg.tater.core.controllers.leaderboard

import gg.tater.shared.leaderboard.LeaderboardService
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer

class LeaderboardController : LeaderboardService {

    override fun setup(consmer: TerminableConsumer) {
        Services.provide(LeaderboardService::class.java, this)
    }
}