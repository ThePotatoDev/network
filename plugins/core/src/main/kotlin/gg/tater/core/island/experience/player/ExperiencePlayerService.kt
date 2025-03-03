package gg.tater.core.island.experience.player

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.UUID

interface ExperiencePlayerService : TerminableModule {

    fun compute(uuid: UUID): RFuture<ExperiencePlayer>

}