package gg.tater.core.island.experience.player

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface ExperiencePlayerService : TerminableModule {

    fun get(uuid: UUID): RFuture<ExperiencePlayer>

    fun save(player: ExperiencePlayer): RFuture<ExperiencePlayer>

}