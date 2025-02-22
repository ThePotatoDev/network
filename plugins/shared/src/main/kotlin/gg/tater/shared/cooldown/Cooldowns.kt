package gg.tater.shared.cooldown

import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import org.redisson.api.RFuture
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

private const val COOLDOWNS_SET_NAME = "cooldowns"

private val redis = Services.load(Redis::class.java).client

fun isCoolingDown(data: Pair<UUID, String>): RFuture<Boolean> {
    return redis.getMapCache<Pair<UUID, String>, Instant>(COOLDOWNS_SET_NAME)
        .containsKeyAsync(data)
}

fun addCooldown(data: Pair<UUID, String>, time: Long, unit: TimeUnit, instant: Instant): RFuture<Instant> {
    return redis.getMapCache<Pair<UUID, String>, Instant>(COOLDOWNS_SET_NAME)
        .putAsync(data, instant, time, unit)
}

fun removeCooldown(data: Pair<UUID, String>): RFuture<Instant> {
    return redis.getMapCache<Pair<UUID, String>, Instant>(COOLDOWNS_SET_NAME)
        .removeAsync(data)
}
