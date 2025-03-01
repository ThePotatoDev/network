package gg.tater.hub

import gg.tater.core.island.player.position.PositionDirector
import gg.tater.core.island.player.position.SpawnPositionData
import gg.tater.core.player.PlayerRedirectRequest
import gg.tater.core.redis.Redis
import gg.tater.core.server.model.ServerType
import gg.tater.oneblock.island.controllers.OneBlockIslandService
import gg.tater.oneblock.player.OneBlockPlayerService
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Gui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*

class HubServerGui(
    private val opener: Player,
    private val islands: OneBlockIslandService = Services.load(OneBlockIslandService::class.java),
    private val players: OneBlockPlayerService = Services.load(OneBlockPlayerService::class.java),
    private val redis: Redis = Services.load(Redis::class.java)
) :
    Gui(opener, 4, "Select A Server") {

    override fun redraw() {
        setItem(
            13, ItemStackBuilder.of(Material.GRASS_BLOCK)
                .name("&aOneBlock &7(Click)")
                .build {
                    players.compute(opener.name, opener.uniqueId).thenAcceptAsync { player ->
                        val island = islands.getIslandFor(player)?.get()

                        // If they don't have a OneBlock yet,
                        // direct them to the FTUE first
                        if (island == null) {
                            val server = redis.getServer(ServerType.ONEBLOCK_SERVER)
                            if (server == null) {
                                opener.sendMessage(Component.text("Could not find server.", NamedTextColor.RED))
                                return@thenAcceptAsync
                            }

                            opener.sendMessage(
                                Component.text("Creating your OneBlock...", NamedTextColor.GRAY)
                                    .decorate(TextDecoration.ITALIC)
                            )
                            islands.createFor(player, server)
                            return@thenAcceptAsync
                        }

                        val lastServerType = player.lastServerType
                        val spawnData = player.getServerSpawnPos(lastServerType) ?: return@thenAcceptAsync

                        when (lastServerType) {
                            // If they were previously on a OneBlock server,
                            // direct them to that OneBlock server again
                            ServerType.ONEBLOCK_SERVER -> {
                                val islandId =
                                    UUID.fromString(spawnData.getMetaValue(SpawnPositionData.ISLAND_ID_META_KEY))
                                val islandFor = islands.getIsland(islandId).get()

                                // If the island no longer exists, send them to spawn
                                // and reset their spawn position data
                                if (islandFor == null) {
                                    player.setServerSpawnPos(
                                        ServerType.ONEBLOCK_SPAWN,
                                        PositionDirector.WORLD_TELEPORT_DIRECTOR,
                                        ServerType.ONEBLOCK_SPAWN.spawn!!
                                    )
                                } else {
                                    islands.directToOccupiedServer(opener, islandFor)
                                }
                            }

                            else -> {
                                val server = redis.getServer(lastServerType)
                                if (server == null) {
                                    opener.sendMessage(Component.text("Could not find server.", NamedTextColor.RED))
                                    return@thenAcceptAsync
                                }

                                redis.publish(PlayerRedirectRequest(opener.uniqueId, lastServerType, server.id))
                            }
                        }
                    }
                })
    }
}