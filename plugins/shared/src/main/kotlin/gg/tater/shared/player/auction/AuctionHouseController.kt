package gg.tater.shared.player.auction

import gg.tater.shared.annotation.Controller
import gg.tater.shared.DECIMAL_FORMAT
import gg.tater.shared.network.server.ServerType
import gg.tater.shared.player.auction.gui.AuctionHouseGui
import gg.tater.shared.player.auction.model.AuctionHouseCategory
import gg.tater.shared.player.auction.model.AuctionHouseItem
import gg.tater.shared.player.auction.model.AuctionHouseSort
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Material
import org.redisson.api.RFuture
import org.redisson.api.map.event.EntryExpiredListener
import java.util.*
import java.util.concurrent.TimeUnit

@Controller(
    id = "auction-house-controller",
    ignoredBinds = [ServerType.HUB]
)
class AuctionHouseController(private val mapName: String) : AuctionHouseService {

    private val expiredAuctionsSetName = "${mapName}_expired_auctions"

    private val redis = Services.load(Redis::class.java)

    init {
        redis.client.getMapCache<UUID, AuctionHouseItem>(mapName)
            .addListenerAsync(EntryExpiredListener {
                saveExpired(it.key, it.value)
            })
    }

    override fun all(): RFuture<Collection<AuctionHouseItem>> {
        return redis.client.getMapCache<UUID, AuctionHouseItem>(mapName)
            .readAllValuesAsync()
    }

    override fun saveExpired(uuid: UUID, item: AuctionHouseItem): RFuture<Boolean> {
        return redis.client.getListMultimap<UUID, AuctionHouseItem>(mapName)
            .putAsync(uuid, item)
    }

    override fun removeExpired(uuid: UUID, item: AuctionHouseItem): RFuture<Long> {
        return redis.client.getListMultimap<UUID, AuctionHouseItem>(mapName)
            .fastRemoveAsync(uuid, item.id)
    }

    override fun getExpired(uuid: UUID): RFuture<Collection<AuctionHouseItem>> {
        return redis.client.getListMultimap<UUID, AuctionHouseItem>(mapName)
            .getAllAsync(uuid)
    }

    override fun save(item: AuctionHouseItem): RFuture<Boolean> {
        return redis.client.getMapCache<UUID, AuctionHouseItem>(mapName)
            .fastPutAsync(item.id, item, 3L, TimeUnit.DAYS)
    }

    override fun delete(item: AuctionHouseItem): RFuture<Long> {
        return redis.client.getMapCache<UUID, AuctionHouseItem>(mapName)
            .fastRemoveAsync(item.id)
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(AuctionHouseService::class.java, this)

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

                if (it.args().isEmpty()) {
                    all().thenAccept { items ->
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

                    save(item)

                    it.reply("&aYou have listed your item to the auction house for $${DECIMAL_FORMAT.format(item.price)}!")
                }
            }
            .registerAndBind(consumer, "auction", "ah", "auctionhouse")
    }
}