package gg.tater.core.controllers.player.leaderboard

import gg.tater.shared.leaderboard.Leaderboard
import me.lucko.helper.terminable.TerminableConsumer

class PlayerBalanceLeaderboard: Leaderboard {

    override fun id(): String {
        return "player_balances"
    }

    override fun setup(consumer: TerminableConsumer) {
        TODO("Not yet implemented")
    }
}