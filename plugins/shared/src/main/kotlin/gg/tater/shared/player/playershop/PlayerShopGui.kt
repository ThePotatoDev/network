package gg.tater.shared.player.playershop

import gg.tater.shared.ARROW_TEXT
import gg.tater.shared.DECIMAL_FORMAT
import gg.tater.shared.redis.Redis
import gg.tater.shared.island.message.placement.IslandPlacementRequest
import gg.tater.shared.network.model.ServerType
import gg.tater.shared.player.position.PlayerPositionResolver
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.paginated.PageInfo
import me.lucko.helper.menu.paginated.PaginatedGui
import me.lucko.helper.menu.paginated.PaginatedGuiBuilder
import me.lucko.helper.menu.scheme.MenuScheme
import me.lucko.helper.menu.scheme.StandardSchemeMappings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

class PlayerShopGui(
    player: Player,
    private val shops: Collection<PlayerShopDataModel>,
    private val redis: Redis,
    private val server: String
) :
    PaginatedGui(
        {
            shops.map { shop ->
                ItemStackBuilder.of(shop.icon)
                    .name(shop.name)
                    .lore(getLore(shop))
                    .build {
                        redis.islands()
                            .getAsync(shop.islandId)
                            .thenAcceptAsync { island ->
                                if (island == null) {
                                    player.sendMessage(
                                        Component.text(
                                            "That island no longer exists where the warp was present.",
                                            NamedTextColor.RED
                                        )
                                    )
                                    return@thenAcceptAsync
                                }

                                if (!shop.open) {
                                    player.sendMessage(
                                        Component.text(
                                            "That warp is not open currently.",
                                            NamedTextColor.RED
                                        )
                                    )
                                    return@thenAcceptAsync
                                }

                                val currentServerId = island.currentServerId
                                val position = shop.position

                                player.sendMessage(
                                    Component.text(
                                        "Warping you to the ${shop.name} shop...",
                                        NamedTextColor.GREEN
                                    )
                                )

                                // If the player is on the same server as the warp
                                if (currentServerId != null && currentServerId == server) {
                                    player.teleportAsync(
                                        Location(
                                            Bukkit.getWorld(island.id.toString()),
                                            position.x,
                                            position.y,
                                            position.z,
                                            position.yaw,
                                            position.pitch
                                        )
                                    )
                                    return@thenAcceptAsync
                                }

                                redis.players().getAsync(player.uniqueId).thenAcceptAsync { data ->
                                    data.setSpawn(ServerType.SERVER, position)
                                    redis.players().fastPut(
                                        player.uniqueId,
                                        data.setPositionResolver(
                                            PlayerPositionResolver.Type.TELEPORT_PLAYER_SHOP,
                                            shop.islandId.toString()
                                        )
                                    )
                                    IslandPlacementRequest.directToActive(redis, player, island)
                                }
                            }
                    }
            }
        }, player,
        BUILDER
    ) {

    companion object {
        private val BUILDER: PaginatedGuiBuilder = PaginatedGuiBuilder
            .create()
            .title("&nPlayer Shops")
            .nextPageSlot(53)
            .previousPageSlot(45)
            .lines(6)
            .scheme(
                MenuScheme(StandardSchemeMappings.STAINED_GLASS)
                    .maskEmpty(5)
                    .mask("111101111")
                    .scheme(0, 0, 0, 0, 0, 0, 0, 0)
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

        private fun getLore(warp: PlayerShopDataModel): List<String> {
            val lore: MutableList<String> = mutableListOf()

            lore.add(" ")
            lore.add(warp.description)
            lore.add(" ")
            lore.add("$ARROW_TEXT &fShop Status: ${if (warp.open) "&aOpen" else "&cClosed"}")
            lore.add("$ARROW_TEXT &fShop Visits: ${DECIMAL_FORMAT.format(warp.visits)}")
            lore.add("$ARROW_TEXT &bClick &fto visit!")
            lore.add(" ")
            return lore
        }
    }

}