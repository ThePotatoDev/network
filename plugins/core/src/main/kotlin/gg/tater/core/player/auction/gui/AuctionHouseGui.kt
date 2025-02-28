package gg.tater.core.player.auction.gui

import gg.tater.core.ARROW_TEXT
import gg.tater.core.DECIMAL_FORMAT
import gg.tater.core.player.auction.AuctionHouseService
import gg.tater.core.player.auction.model.AuctionHouseCategory
import gg.tater.core.player.auction.model.AuctionHouseItem
import gg.tater.core.player.auction.model.AuctionHouseScope
import gg.tater.core.player.auction.model.AuctionHouseSort
import gg.tater.core.player.economy.model.EconomyType
import gg.tater.core.player.economy.model.PlayerEconomyService
import gg.tater.core.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Item
import me.lucko.helper.menu.paginated.PageInfo
import me.lucko.helper.menu.paginated.PaginatedGui
import me.lucko.helper.menu.paginated.PaginatedGuiBuilder
import me.lucko.helper.menu.scheme.MenuScheme
import me.lucko.helper.menu.scheme.StandardSchemeMappings
import me.lucko.helper.time.DurationFormatter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class AuctionHouseGui(
    items: Collection<AuctionHouseItem>,
    player: Player,
    private val redis: Redis,
    private val category: AuctionHouseCategory,
    private val sort: AuctionHouseSort,
    private var scope: AuctionHouseScope = AuctionHouseScope.NONE
) :
    PaginatedGui(
        {
            getItems(player, items, redis, it, category, sort, scope)
        }, player, BUILDER
    ) {

    companion object {
        private val BUILDER: PaginatedGuiBuilder = PaginatedGuiBuilder
            .create()
            .title("&nAuction House")
            .nextPageSlot(53)
            .previousPageSlot(45)
            .lines(6)
            .scheme(
                MenuScheme(StandardSchemeMappings.STAINED_GLASS)
                    .maskEmpty(5)
                    .mask("110001101")
                    .scheme(0, 0, 0, 0, 0)
            )
            .nextPageItem { info: PageInfo ->
                ItemStackBuilder.of(Material.PAPER)
                    .name("&6Next Page &f(" + info.current + "/" + info.size + ")")
                    .lore("&7&oClick to advance.")
                    .build()
            }
            .previousPageItem { info: PageInfo ->
                ItemStackBuilder.of(Material.PAPER)
                    .name("&cPrevious Page &f(" + info.current + "/" + info.size + ")")
                    .lore("&7&oClick to return.")
                    .build()
            }
            .itemSlots(
                MenuScheme()
                    .mask("111111111")
                    .mask("111111111")
                    .mask("111111111")
                    .mask("111111111")
                    .mask("111111111")
                    .mask("000000000")
                    .maskedIndexesImmutable
            )

        private fun getItems(
            player: Player,
            items: Collection<AuctionHouseItem>,
            redis: Redis,
            gui: PaginatedGui,
            category: AuctionHouseCategory,
            sort: AuctionHouseSort,
            scope: AuctionHouseScope,
            ecoService: PlayerEconomyService = Services.load(PlayerEconomyService::class.java),
            auctionService: AuctionHouseService = Services.load(AuctionHouseService::class.java),
        ): List<Item> {
            if (scope == AuctionHouseScope.NONE) {
                gui.setItem(
                    47, ItemStackBuilder.of(Material.PLAYER_HEAD)
                        .name("&bYour Auction Page")
                        .lore(
                            " ",
                            "&7View the following sections:",
                            "$ARROW_TEXT &fYour Active Listings",
                            "$ARROW_TEXT &fYour Expired Items",
                            " ",
                            "&bClick &fto view!"
                        )
                        .transformMeta { meta ->
                            val skull = meta as SkullMeta
                            skull.setOwningPlayer(player)
                        }
                        .build {
                            AuctionHouseSelectionGui(player, redis).open()
                        })
            } else {
                gui.setItem(
                    47, ItemStackBuilder.of(Material.WHITE_STAINED_GLASS_PANE)
                        .name(" ")
                        .build(null)
                )

                gui.setFallbackGui { AuctionHouseSelectionGui(player, redis) }
            }

            gui.setItem(
                48, ItemStackBuilder.of(Material.CLOCK)
                    .name("&bCategory: &f${category.friendly}")
                    .lore(getCategoryLore(category))
                    .build {
                        val next = category.next()
                        gui.updateContent(getItems(player, items, redis, gui, next, sort, scope))
                        gui.redraw()
                    })

            gui.setItem(
                49, ItemStackBuilder.of(Material.WRITTEN_BOOK)
                    .name("&bHow do I list an item?")
                    .lore(
                        "&7Use /ah sell <price> to list an item.",
                        " ",
                        "&bHow do I remove a listing?",
                        "&7Right-Click a listing you own",
                        "&7to remove it from the auction house.",
                        " ",
                        "&bWhere do my expired items go?",
                        "&7Expired auction house items go",
                        "&7into the expired items section of",
                        "&7your auction house profile.",
                    )
                    .build(null)
            )

            gui.setItem(
                52, ItemStackBuilder.of(Material.HOPPER)
                    .name("&bSort By: &f${sort.friendly}")
                    .lore(getSortLore(sort))
                    .build {
                        val next = sort.next()
                        gui.updateContent(getItems(player, items, redis, gui, category, next, scope))
                        gui.redraw()
                    })

            return items.sortedWith(sort.comparator)
                .filter { item ->
                    if (category.validator == null) return@filter true
                    return@filter category.validator.test(item)
                }.filter { item ->
                    if (scope == AuctionHouseScope.NONE) return@filter true
                    return@filter item.ownerId == player.uniqueId
                }.map { item ->
                    val stack = item.stack.clone()

                    ItemStackBuilder.of(stack)
                        .showAttributes()
                        .lore(getLore(player, stack, item, scope))
                        .build({
                            if (player.uniqueId != item.ownerId) {
                                return@build
                            }

                            val inventory = player.inventory
                            if (inventory.firstEmpty() == -1) {
                                player.sendMessage(
                                    Component.text(
                                        "Please make space in your inventory for this item!",
                                        NamedTextColor.RED
                                    )
                                )
                                return@build
                            }

                            gui.close()

                            inventory.addItem(item.stack)

                            if (scope != AuctionHouseScope.PERSONAL_EXPIRED_LISTINGS) {
                                auctionService.delete(item)
                            } else {
                                auctionService.removeExpired(player.uniqueId, item)
                            }

                            player.sendMessage(
                                Component.text(
                                    "Item has been successfully removed from the auction house.",
                                    NamedTextColor.RED
                                )
                            )
                        }, {
                            gui.close()

                            if (item.ownerId == player.uniqueId) {
                                player.sendMessage(
                                    Component.text(
                                        "You cannot purchase your own auction house item!",
                                        NamedTextColor.RED
                                    )
                                )
                                return@build
                            }

                            ecoService.get(player.uniqueId).thenAccept { eco ->
                                val balance = eco.get(EconomyType.MONEY)

                                if (balance - item.price < 0) {
                                    player.sendMessage(
                                        Component.text(
                                            "You do not have enough money to purchase this! ($${
                                                DECIMAL_FORMAT.format(
                                                    item.price
                                                )
                                            })",
                                            NamedTextColor.RED
                                        )
                                    )
                                    return@thenAccept
                                }

                                val inventory = player.inventory
                                if (inventory.firstEmpty() == -1) {
                                    player.sendMessage(
                                        Component.text(
                                            "Please make space in your inventory for this item!",
                                            NamedTextColor.RED
                                        )
                                    )
                                    return@thenAccept
                                }

                                inventory.addItem(item.stack)

                                auctionService.delete(item)

                                eco.withdraw(EconomyType.MONEY, item.price)
                                ecoService.save(player.uniqueId, eco)

                                player.sendMessage(
                                    Component.text(
                                        "Successfully purchased item from the auction house!",
                                        NamedTextColor.GREEN
                                    )
                                )
                            }
                        })
                }
        }

        private fun getLore(
            player: Player,
            stack: ItemStack,
            item: AuctionHouseItem,
            scope: AuctionHouseScope?
        ): List<String> {
            val lore: MutableList<String> = mutableListOf()
            val stackLore = stack.lore()

            if (stackLore != null) {
                lore.addAll(stackLore.map { LegacyComponentSerializer.legacyAmpersand().serialize(it) })
            }

            lore.add(" ")
            lore.add("&bAuction Details:")
            lore.add("$ARROW_TEXT &bPoster: &f${item.ownerName}")
            lore.add("$ARROW_TEXT &bPrice: &f$${DECIMAL_FORMAT.format(item.price)}")
            lore.add(
                "$ARROW_TEXT &bExpires In: &f${
                    DurationFormatter.format(
                        Duration.between(
                            Instant.now(),
                            Instant.ofEpochMilli(item.listedAt).plus(3L, ChronoUnit.DAYS)
                        ), true
                    )
                }"
            )
            lore.add(" ")

            if (scope == null && item.ownerId != player.uniqueId) {
                lore.add("&bLeft-Click &fto &apurchase&f!")
            }

            if (scope != AuctionHouseScope.PERSONAL_EXPIRED_LISTINGS && item.ownerId == player.uniqueId) {
                lore.add("&bRight-Click &fto &cremove&f!")
            }

            if (scope == AuctionHouseScope.PERSONAL_EXPIRED_LISTINGS) {
                lore.add("&bRight-Click &fto &areclaim&f!")
            }

            return lore
        }

        private fun getCategoryLore(selected: AuctionHouseCategory): List<String> {
            val lore: MutableList<String> = mutableListOf()
            lore.add(" ")

            for (category in AuctionHouseCategory.entries) {
                if (selected == category) {
                    lore.add("$ARROW_TEXT &a${category.friendly}")
                } else {
                    lore.add("$ARROW_TEXT &7${category.friendly}")
                }
            }

            lore.add(" ")
            lore.add("&bClick &fto cycle!")
            return lore
        }

        private fun getSortLore(selected: AuctionHouseSort): List<String> {
            val lore: MutableList<String> = mutableListOf()
            lore.add(" ")

            for (sort in AuctionHouseSort.entries) {
                if (selected == sort) {
                    lore.add("$ARROW_TEXT &a${sort.friendly}")
                } else {
                    lore.add("$ARROW_TEXT &7${sort.friendly}")
                }
            }

            lore.add(" ")
            lore.add("&bClick &fto cycle!")
            return lore
        }
    }
}