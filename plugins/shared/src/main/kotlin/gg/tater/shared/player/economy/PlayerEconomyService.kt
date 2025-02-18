package gg.tater.shared.player.economy

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import org.redisson.api.map.event.EntryCreatedListener
import java.util.*

interface PlayerEconomyService : TerminableModule {

    fun compute(uuid: UUID): RFuture<PlayerEconomyModel>

    fun get(uuid: UUID): RFuture<PlayerEconomyModel>

    fun getSync(uuid: UUID): PlayerEconomyModel?

    fun save(uuid: UUID, eco: PlayerEconomyModel): RFuture<Boolean>

    fun onCreated(action: (UUID, PlayerEconomyModel) -> Unit): RFuture<Int>

    fun onUpdated(action: (UUID, PlayerEconomyModel) -> Unit): RFuture<Int>

}