package gg.tater.core.controllers.planet

import gg.tater.core.controllers.planet.model.PlanetPlayerData
import gg.tater.shared.annotation.Controller
import gg.tater.shared.server.model.ServerType
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.redisson.api.RFuture
import java.util.*

@Controller(
    id = "planet-controller",
    ignoredBinds = [ServerType.HUB]
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