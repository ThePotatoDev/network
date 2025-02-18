package gg.tater.shared.player.economy.leaderboard

import gg.tater.shared.leaderboard.Leaderboard
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.player.economy.PlayerEconomyService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.redisson.api.RFuture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class PlayerBalanceLeaderboard : Leaderboard<PlayerDataModel> {

    private companion object {
        const val LEADERBOARD_ID = "player-balance-leaderboard"
        const val PLAYER_BALANCE_CACHE_NAME = "player-balance-cache"
        const val MAX_LEADEBOARD_CACHE_SIZE = 10
    }

    private val redis = Services.load(Redis::class.java)
    private val set = redis.client.getScoredSortedSet<PlayerDataModel>(PLAYER_BALANCE_CACHE_NAME)

    override fun id(): String {
        return LEADERBOARD_ID
    }

    override fun refreshTime(): Duration {
        return 1.hours
    }

    override fun compute(): RFuture<Collection<PlayerDataModel>> {
        return set.valueRangeReversedAsync(0, MAX_LEADEBOARD_CACHE_SIZE - 1)
    }

    override fun setup(consumer: TerminableConsumer) {
        val economy = Services.load(PlayerEconomyService::class.java)

        // Update the sorted set when a players balance is updated
        economy.onUpdated { _, player ->

        }

        // Update the sorted set when a players balance data is created
        economy.onCreated { _, player ->

        }
    }
}