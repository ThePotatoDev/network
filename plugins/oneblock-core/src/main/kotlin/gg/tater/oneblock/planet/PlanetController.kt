package gg.tater.oneblock.planet

import gg.tater.oneblock.planet.model.PlanetPlayerData
import gg.tater.shared.annotation.Controller
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.redisson.api.RFuture
import java.util.*

@Controller(
    id = "planet-controller"
)
class PlanetController : PlanetService {

    private companion object {

    }

    private val redis = Services.load(Redis::class.java)

    override fun compute(uuid: UUID): RFuture<PlanetPlayerData> {
        TODO("Not yet implemented")
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(PlanetService::class.java, this)
    }
}