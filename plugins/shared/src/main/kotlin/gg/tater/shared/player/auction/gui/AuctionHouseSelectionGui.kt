package gg.tater.shared.player.auction.gui

import gg.tater.shared.ARROW_TEXT
import gg.tater.shared.player.auction.AuctionHouseService
import gg.tater.shared.player.auction.model.AuctionHouseCategory
import gg.tater.shared.player.auction.model.AuctionHouseScope
import gg.tater.shared.player.auction.model.AuctionHouseSort
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Gui
import org.bukkit.Material
import org.bukkit.entity.Player

class AuctionHouseSelectionGui(
    player: Player,
    private val redis: Redis,
    private val service: AuctionHouseService = Services.load(AuctionHouseService::class.java)
) : Gui(player, 1, "&nSelect Option") {

    override fun redraw() {
        for (i in 0 until this.handle.size) {
            setItem(
                i, ItemStackBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build(null)
            )
        }

        setItem(
            3, ItemStackBuilder.of(Material.ENDER_CHEST)
                .name("&bYour Active Listings")
                .lore(
                    " ",
                    "$ARROW_TEXT &bClick &fto view!",
                    " "
                )
                .build {
                    service.all().thenApplyAsync { it.filter { item -> item.ownerId == player.uniqueId } }
                        .thenAccept { items ->
                            AuctionHouseGui(
                                items,
                                player,
                                redis,
                                AuctionHouseCategory.ALL,
                                AuctionHouseSort.DATE_POSTED,
                                AuctionHouseScope.PERSONAL_LISTINGS
                            ).open()
                        }
                })

        setItem(
            5, ItemStackBuilder.of(Material.ENDER_EYE)
                .name("&bYour Expired Listings")
                .lore(
                    " ",
                    "$ARROW_TEXT &bClick &fto view!",
                    " "
                )
                .build {
                    service.getExpired(player.uniqueId)
                        .thenAccept { items ->
                            AuctionHouseGui(
                                items,
                                player,
                                redis,
                                AuctionHouseCategory.ALL,
                                AuctionHouseSort.DATE_POSTED,
                                AuctionHouseScope.PERSONAL_EXPIRED_LISTINGS
                            ).open()
                        }
                })
    }
}