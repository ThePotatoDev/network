package gg.tater.shared.leaderboard

import me.lucko.helper.terminable.module.TerminableModule
import kotlin.time.Duration

interface Leaderboard : TerminableModule {

    fun id(): String

    fun refreshTime(): Duration

}