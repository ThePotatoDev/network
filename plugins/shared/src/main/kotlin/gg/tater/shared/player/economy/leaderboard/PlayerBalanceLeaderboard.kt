package gg.tater.shared.player.economy.leaderboard

import gg.tater.shared.leaderboard.Leaderboard
import gg.tater.shared.player.economy.PlayerEconomyService
import gg.tater.shared.player.economy.message.EconomyBalanceUpdateMessage
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
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
    private val semaphore = redis.client.getSemaphore("")

    override fun id(): String {
        return LEADERBOARD_ID
    }

    override fun refreshTime(): Duration {
        return 1.hours
    }

    override fun compute(amount: Int): List<Pair<UUID, Double>> {
        return set.entryRangeReversed(0, amount - 1).map { it.value to it.score }
    }

    override fun setup(consumer: TerminableConsumer) {
        val economy = Services.load(PlayerEconomyService::class.java)

        redis.listen<EconomyBalanceUpdateMessage> {
            if (semaphore.tryAcquire()) {
                try {
                    val uuid = it.uuid
                    val newBalance = it.newBalance
                    set.add(newBalance, uuid)
                } finally {
                    semaphore.release()
                }
            }
        }
    }
}