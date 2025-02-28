package gg.tater.core.island.player

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface IslandPlayerService<T : IslandPlayer> : TerminableModule {

    fun compute(name: String, uuid: UUID): RFuture<T>

    fun get(uuid: UUID): RFuture<T>

    fun save(data: T): RFuture<Boolean>

    fun transaction(data: IslandPlayer, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})

}