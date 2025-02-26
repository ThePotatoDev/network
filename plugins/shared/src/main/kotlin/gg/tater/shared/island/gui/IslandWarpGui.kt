package gg.tater.shared.island.gui

import gg.tater.shared.ARROW_TEXT
import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.server.ServerDataService
import gg.tater.shared.server.model.ServerType
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

class IslandWarpGui<T : Island>(
    opener: Player,
    private val island: T
) :
    PaginatedGui(
        {
            getItems(it, opener, island)
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

        private fun <T : Island> getItems(
            gui: PaginatedGui,
            opener: Player,
            island: T,
            server: String = Services.load(ServerDataService::class.java).id(),
            players: PlayerService = Services.load(PlayerService::class.java),
            islands: IslandService<T> = Services.load(IslandService::class.java) as IslandService<T>
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
                            player.setSpawn(ServerType.SERVER, warp)

                            players.transaction(
                                player.setPositionResolver(PlayerPositionResolver.Type.TELEPORT_ISLAND_WARP),
                                onSuccess = {
                                    islands.directToOccupiedServer(opener, island)
                                })
                        }
                    }
            }
        }
    }
}