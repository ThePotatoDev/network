package gg.tater.shared.leaderboard

import me.lucko.helper.terminable.module.TerminableModule

interface Leaderboard: TerminableModule {

    fun id(): String

}