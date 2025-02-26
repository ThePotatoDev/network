package gg.tater.core.controllers.server.hub

import gg.tater.shared.island.IslandService
import gg.tater.shared.server.model.ServerType
import gg.tater.shared.player.PlayerService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Gui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player

class HubServerGui(
    private val opener: Player,
    private val islands: IslandService = Services.load(IslandService::class.java),
    private val players: PlayerService = Services.load(PlayerService::class.java),
    private val redis: Redis = Services.load(Redis::class.java)
) :
    Gui(opener, 4, "Select A Server") {

    override fun redraw() {
        setItem(
            13, ItemStackBuilder.of(Material.GRASS_BLOCK)
                .name("&aOneBlock &7(Click)")
                .build {
                    players.get(opener.uniqueId).thenAcceptAsync { player ->
                        val island = islands.getIslandFor(player)?.get()

                        /**
                         * If they have an island, send them to their old server type
                         * associated with OneBlock.
                         */
                        if (island != null) {
                            return@thenAcceptAsync
                        }

                        val server = redis.getServer(ServerType.SERVER)
                        if (server == null) {
                            opener.sendMessage(Component.text("Could not find server.", NamedTextColor.RED))
                            return@thenAcceptAsync
                        }

                        opener.sendMessage(
                            Component.text("Creating your OneBlock...", NamedTextColor.GRAY)
                                .decorate(TextDecoration.ITALIC)
                        )
                        islands.createFor(player, server)
                    }
                })
    }
}