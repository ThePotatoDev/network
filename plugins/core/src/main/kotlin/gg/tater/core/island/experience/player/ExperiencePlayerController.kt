package gg.tater.core.island.experience.player

import gg.tater.core.redis.Redis
import gg.tater.core.server.model.GameModeType
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.redisson.api.RFuture
import java.util.*

class ExperiencePlayerController(mode: GameModeType) : ExperiencePlayerService {

    private val mapName = "${mode.id}_experience_players"

    private val redis = Services.load(Redis::class.java)

    override fun get(uuid: UUID): RFuture<ExperiencePlayer> {
        return redis.client.getMap<UUID, ExperiencePlayer>(mapName)
            .computeIfAbsentAsync(uuid) {
                ExperiencePlayer(uuid)
            }
    }

    override fun save(player: ExperiencePlayer): RFuture<ExperiencePlayer> {
        return redis.client.getMap<UUID, ExperiencePlayer>(mapName)
            .putAsync(player.uuid, player)
    }

    override fun setup(consumer: TerminableConsumer) {

    }
}