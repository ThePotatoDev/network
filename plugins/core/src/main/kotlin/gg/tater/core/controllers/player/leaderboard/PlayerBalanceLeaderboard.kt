package gg.tater.core.controllers.player.leaderboard

import gg.tater.shared.leaderboard.Leaderboard
import me.lucko.helper.terminable.TerminableConsumer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class PlayerBalanceLeaderboard: Leaderboard {

    override fun id(): String {
        return "player_balances"
    }

    override fun refreshTime(): Duration {
        return 1.hours
    }

    override fun setup(consumer: TerminableConsumer) {
        TODO("Not yet implemented")
    }
}