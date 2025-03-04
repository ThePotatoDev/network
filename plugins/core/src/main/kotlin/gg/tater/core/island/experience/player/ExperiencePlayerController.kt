package gg.tater.core.island.experience.player

import gg.tater.core.redis.Redis
import gg.tater.core.server.model.GameModeType
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.redisson.api.RFuture
import java.util.*

class ExperiencePlayerController(val mode: GameModeType) : ExperiencePlayerService {

    private val redis = Services.load(Redis::class.java)

    override fun get(uuid: UUID): RFuture<ExperiencePlayer> {
        TODO("Not yet implemented")
    }

    override fun save(player: ExperiencePlayer): RFuture<Boolean> {
        TODO("Not yet implemented")
    }

    override fun setup(consumer: TerminableConsumer) {
        TODO("Not yet implemented")
    }
}