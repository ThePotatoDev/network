package gg.tater.shared.player.economy.leaderboard

import gg.tater.shared.leaderboard.Leaderboard
import gg.tater.shared.player.economy.message.EconomyBalanceUpdateMessage
import gg.tater.shared.player.position.WrappedPosition
import gg.tater.shared.redis.Redis
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class PlayerBalanceLeaderboard : Leaderboard<UUID, Double> {

    private companion object {
        const val LEADERBOARD_ID = "player-balance-leaderboard"
        const val PLAYER_BALANCE_CACHE_NAME = "player-balance-cache"
        const val LEADERBOARD_COMPUTE_SEMAPHORE = "player-balance-compute"
        const val MAX_LEADEBOARD_CACHE_SIZE = 10
    }

    init {
        Leaderboard.register(this)
    }

    private val redis = Services.load(Redis::class.java)
    private val set = redis.client.getScoredSortedSet<UUID>(PLAYER_BALANCE_CACHE_NAME)
    private val semaphore = redis.client.getSemaphore(LEADERBOARD_COMPUTE_SEMAPHORE)

    override fun title(): Component {
        return Component.text("Top Player Balances", NamedTextColor.DARK_AQUA)
            .decorate(TextDecoration.BOLD)
    }

    override fun format(): Component {
        return Component.text("<number>. <player>: $<balance>")
    }

    override fun id(): String {
        return LEADERBOARD_ID
    }

    override fun refreshTime(): Duration {
        return 1.hours
    }

    override fun amount(): Int {
        return MAX_LEADEBOARD_CACHE_SIZE
    }

    override fun compute(amount: Int): List<Pair<UUID, Double>> {
        return set.entryRangeReversed(0, amount - 1).map { it.value to it.score }
    }

    override fun setup(consumer: TerminableConsumer) {
        Schedulers.async().run {
            for (pair in compute(MAX_LEADEBOARD_CACHE_SIZE)) {
                val uuid = pair.first
                val balance = pair.second

            }
        }

        consumer.bind(AutoCloseable {

        })

        redis.listen<EconomyBalanceUpdateMessage> {
            if (semaphore.tryAcquire()) {
                try {
                    val uuid = it.uuid
                    val newBalance = it.newBalance
                    set.add(newBalance, uuid)

                    // Trim to keep only top 10 highest balances
                    if (set.size() > MAX_LEADEBOARD_CACHE_SIZE) {
                        val entriesToRemove = set.valueRange(0, set.size() - (MAX_LEADEBOARD_CACHE_SIZE + 1))
                        set.removeAll(entriesToRemove)
                    }
                } finally {
                    semaphore.release()
                }
            }
        }
    }
}