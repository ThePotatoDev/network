package gg.tater.core.island.gui

import gg.tater.core.ARROW_TEXT
import gg.tater.core.island.Island
import gg.tater.core.island.IslandService
import gg.tater.core.island.player.IslandPlayer
import gg.tater.core.island.player.IslandPlayerService
import gg.tater.core.island.player.position.PositionDirector
import gg.tater.core.server.ServerDataService
import gg.tater.core.server.model.ServerType
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Item
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

class IslandWarpGui<T : Island, K : IslandPlayer>(
    opener: Player,
    private val island: T,
    private val players: IslandPlayerService<K> = Services.load(IslandPlayerService::class.java) as IslandPlayerService<K>,
    private val islands: IslandService<T, K> = Services.load(IslandService::class.java) as IslandService<T, K>
) :
    PaginatedGui(
        {
            getItems(it, opener, island, players, islands)
        }, opener,
        BUILDER
    ) {

    companion object {
        private val BUILDER: PaginatedGuiBuilder = PaginatedGuiBuilder
            .create()
            .title("&nIsland Warps")
            .nextPageSlot(53)
            .previousPageSlot(45)
            .lines(6)
            .scheme(
                MenuScheme(StandardSchemeMappings.STAINED_GLASS)
                    .maskEmpty(5)
                    .mask("111111111")
                    .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0)
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

        private fun <T : Island, K : IslandPlayer> getItems(
            gui: PaginatedGui,
            opener: Player,
            island: T,
            players: IslandPlayerService<K>,
            islands: IslandService<T, K>,
            server: String = Services.load(ServerDataService::class.java).id()
        ): List<Item> {
            return island.warps.map {
                val name = it.key

                ItemStackBuilder.of(Material.NETHER_STAR)
                    .name("&6$name Warp")
                    .lore(
                        " ",
                        "$ARROW_TEXT &fClick to Teleport!",
                        " ",
                    )
                    .build {
                        gui.close()

                        val warp = it.value
                        val currentServerId = island.currentServerId

                        opener.sendMessage(Component.text("Warping you to $name...", NamedTextColor.GREEN))

                        // If the player is on the same server as the warp
                        if (currentServerId != null && currentServerId == server) {
                            opener.teleportAsync(
                                Location(
                                    Bukkit.getWorld(island.id.toString()),
                                    warp.x,
                                    warp.y,
                                    warp.z,
                                    warp.yaw,
                                    warp.pitch
                                )
                            )
                            return@build
                        }

                        val uuid = opener.uniqueId
                        players.get(uuid).thenAcceptAsync { player ->
                            players.transaction(
                                player.setNextServerSpawnPos(
                                    ServerType.ONEBLOCK_SERVER,
                                    PositionDirector.ISLAND_TELEPORT_DIRECTOR,
                                    warp
                                ),
                                onSuccess = {
                                    islands.directToOccupiedServer(opener, island)
                                })
                        }
                    }
            }
        }
    }
}