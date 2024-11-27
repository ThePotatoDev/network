package gg.tater.core.controllers.player.auction

import gg.tater.shared.DECIMAL_FORMAT
import gg.tater.shared.redis.Redis
import gg.tater.shared.player.auction.AuctionHouseCategory
import gg.tater.shared.player.auction.AuctionHouseItem
import gg.tater.shared.player.auction.AuctionHouseSort
import gg.tater.shared.player.auction.gui.AuctionHouseGui
import me.lucko.helper.Commands
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Material
import org.redisson.api.map.event.EntryExpiredListener
import java.util.concurrent.TimeUnit

class AuctionHouseController(private val redis: Redis) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        // Listen for item expirations for auction house
        redis.auctions().addListenerAsync(EntryExpiredListener {
            redis.expiredAuctions().putAsync(it.key, it.value)
        })

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

                if (it.args().isEmpty()) {
                    redis.auctions().readAllValuesAsync().thenAccept { items ->
                        AuctionHouseGui(
                            items,
                            sender,
                            redis,
                            AuctionHouseCategory.ALL,
                            AuctionHouseSort.DATE_POSTED
                        ).open()
                    }
                    return@handler
                }

                val sub = it.arg(0).parseOrFail(String::class.java)
                if (sub.equals("sell", true) || sub.equals("list", true)) {
                    if (it.args().size < 2) {
                        it.reply("&cUsage: /ah sell <price>")
                        return@handler
                    }

                    val hand = sender.inventory.itemInMainHand
                    if (hand.type == Material.AIR) {
                        it.reply("&cPlease use a valid item to list!")
                        return@handler
                    }

                    val price = it.arg(1).parseOrFail(String::class.java).toDouble()
                    val item = AuctionHouseItem(sender, price)

                    sender.inventory.setItemInMainHand(null)
                    sender.updateInventory()

                    redis.auctions().putAsync(item.id, item, 3L, TimeUnit.DAYS)

                    it.reply("&aYou have listed your item to the auction house for $${DECIMAL_FORMAT.format(item.price)}!")
                }
            }
            .registerAndBind(consumer, "auction", "ah", "auctionhouse")
    }
}