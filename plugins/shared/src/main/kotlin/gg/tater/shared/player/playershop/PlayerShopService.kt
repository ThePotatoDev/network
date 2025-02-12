package gg.tater.shared.player.playershop

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface PlayerShopService: TerminableModule {

    fun all(): RFuture<Collection<PlayerShopDataModel>>

    fun get(uuid: UUID): RFuture<PlayerShopDataModel>

    fun save(uuid: UUID, shop: PlayerShopDataModel): RFuture<Boolean>

    fun delete(uuid: UUID): RFuture<Long>

}