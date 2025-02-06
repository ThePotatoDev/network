package gg.tater.core.controllers.island.leaderboard

import gg.tater.shared.leaderboard.Leaderboard
import me.lucko.helper.terminable.TerminableConsumer

class IslandValueLeaderboard: Leaderboard {

    override fun id(): String {
        return "island_value"
    }

    override fun setup(consumer: TerminableConsumer) {
        TODO("Not yet implemented")
    }
}