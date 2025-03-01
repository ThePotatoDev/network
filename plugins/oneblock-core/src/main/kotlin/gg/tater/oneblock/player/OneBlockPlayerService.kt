package gg.tater.oneblock.player

import gg.tater.core.island.player.IslandPlayerService
import gg.tater.core.redis.Redis
import gg.tater.core.redis.transactional
import gg.tater.core.server.model.ONEBLOCK_GAMEMODE_SERVERS
import gg.tater.core.server.model.ServerType
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.redisson.api.RFuture
import java.util.*

class OneBlockPlayerService(val serverType: ServerType) : IslandPlayerService<OneBlockPlayer> {

    private companion object {
        const val PLAYER_MAP_NAME = "oneblock_players"
    }

    private val redis = Services.load(Redis::class.java)

    override fun compute(name: String, uuid: UUID): RFuture<OneBlockPlayer> {
        return redis.client.getMap<UUID, OneBlockPlayer>(PLAYER_MAP_NAME)
            .computeIfAbsentAsync(uuid) {
                OneBlockPlayer(uuid, name)
            }
    }

    override fun get(uuid: UUID): RFuture<OneBlockPlayer> {
        return redis.client.getMap<UUID, OneBlockPlayer>(PLAYER_MAP_NAME)
            .getAsync(uuid)
    }

    override fun transaction(data: OneBlockPlayer, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        redis.client.apply {
            this.getMap<UUID, OneBlockPlayer>(PLAYER_MAP_NAME)
                .transactional({ map -> map[data.uuid] = data }, onSuccess, onFailure)
        }
    }

    override fun save(data: OneBlockPlayer): RFuture<OneBlockPlayer> {
        return redis.client.getMap<UUID, OneBlockPlayer>(PLAYER_MAP_NAME)
            .putAsync(data.uuid, data)
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(OneBlockPlayerService::class.java, this)
        Services.provide(IslandPlayerService::class.java, this)

        // Only register spawn handlers on OneBlock related servers
        if (ONEBLOCK_GAMEMODE_SERVERS.contains(serverType)) {
            registerPositionListeners(consumer)
        }
    }
}