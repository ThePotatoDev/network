package gg.tater.shared.leaderboard

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import kotlin.time.Duration

interface Leaderboard<K> : TerminableModule {

    companion object {
        private val LEADERBOARDS = mutableMapOf<String, Leaderboard<*>>()

        fun register(leaderboard: Leaderboard<*>) {
            LEADERBOARDS[leaderboard.id()] = leaderboard
        }
    }

    fun id(): String

    fun refreshTime(): Duration

    fun compute(): RFuture<Collection<K>>

}