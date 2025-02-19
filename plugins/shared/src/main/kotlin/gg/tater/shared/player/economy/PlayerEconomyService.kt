package gg.tater.shared.player.economy

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface PlayerEconomyService : TerminableModule {

    fun compute(uuid: UUID): RFuture<PlayerEconomyModel>

    fun get(uuid: UUID): RFuture<PlayerEconomyModel>

    fun getSync(uuid: UUID): PlayerEconomyModel?

    fun save(uuid: UUID, eco: PlayerEconomyModel): RFuture<Boolean>

}