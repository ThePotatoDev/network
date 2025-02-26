package gg.tater.core.controllers.planet

import gg.tater.core.controllers.planet.model.PlanetPlayerData
import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface PlanetService: TerminableModule {

    fun compute(uuid: UUID): RFuture<PlanetPlayerData>

}