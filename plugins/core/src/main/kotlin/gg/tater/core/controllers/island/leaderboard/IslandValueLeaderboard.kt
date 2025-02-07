package gg.tater.core.controllers.island.leaderboard

import gg.tater.shared.leaderboard.Leaderboard
import me.lucko.helper.terminable.TerminableConsumer
import kotlin.time.Duration

class IslandValueLeaderboard: Leaderboard {

    override fun id(): String {
        return "island_value"
    }

    override fun refreshTime(): Duration {
        TODO("Not yet implemented")
    }

    override fun setup(consumer: TerminableConsumer) {
        TODO("Not yet implemented")
    }
}