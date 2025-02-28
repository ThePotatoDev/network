package gg.tater.core.player.auction

import gg.tater.core.player.auction.model.AuctionHouseItem
import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface AuctionHouseService : TerminableModule {

    fun all(): RFuture<Collection<AuctionHouseItem>>

    fun saveExpired(uuid: UUID, item: AuctionHouseItem): RFuture<Boolean>

    fun removeExpired(uuid: UUID, item: AuctionHouseItem): RFuture<Long>

    fun getExpired(uuid: UUID): RFuture<Collection<AuctionHouseItem>>

    fun save(item: AuctionHouseItem): RFuture<Boolean>

    fun delete(item: AuctionHouseItem): RFuture<Long>

}