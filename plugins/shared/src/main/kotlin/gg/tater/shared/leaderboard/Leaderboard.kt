package gg.tater.shared.leaderboard

import gg.tater.shared.annotation.InvocationContext
import gg.tater.shared.annotation.InvocationContextType
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import kotlin.time.Duration

interface Leaderboard<K, V> : TerminableModule {

    companion object {
        private val LEADERBOARDS = mutableMapOf<String, Leaderboard<*, *>>()

        fun register(leaderboard: Leaderboard<*, *>) {
            LEADERBOARDS[leaderboard.id()] = leaderboard
        }

        fun all(): Collection<Leaderboard<*, *>> {
            return LEADERBOARDS.values
        }
    }

    fun title(): Component

    fun format(): Component

    fun id(): String

    fun refreshTime(): Duration

    fun amount(): Int

    @InvocationContext(type = InvocationContextType.ASYNC)
    fun compute(amount: Int): List<Pair<K, V>>

}