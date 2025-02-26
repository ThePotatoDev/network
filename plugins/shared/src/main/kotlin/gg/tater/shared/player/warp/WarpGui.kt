package gg.tater.shared.player.warp

import gg.tater.shared.player.PlayerRedirectRequest
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.getCurrentServerType
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.redis.Redis
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.menu.Gui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class WarpGui(
    private val opener: Player,
    private val redis: Redis,
    private val players: PlayerService = Services.load(PlayerService::class.java)
) :
    Gui(opener, 3, "Server Warps") {

    override fun redraw() {
        for (warp in WarpType.entries) {
            setItem(
                warp.slot, warp.icon.clearLore()
                    .lore(getLore(warp))
                    .build {
                        val serverType = warp.serverType

                        players.get(opener.uniqueId).thenAccept { player ->
                            Schedulers.sync().run {
                                if (player != null) {
                                    println("not null")
                                }

                                // If the player is already on the warp's server
                                if (player.getCurrentServerType() == warp.serverType) {
                                    val position = ServerType.ONEBLOCK_SPAWN!!

                                    val location = Location(
                                        Bukkit.getWorld("world"),
                                        position.x,
                                        position.y,
                                        position.z,
                                        position.yaw,
                                        position.pitch
                                    )

                                    opener.teleportAsync(
                                        location
                                    )
                                } else {
                                    player.setSpawn(serverType, ServerType.ONEBLOCK_SPAWN!!)

                                    Schedulers.async().run {
                                        players.transaction(
                                            player.setPositionResolver(PlayerPositionResolver.Type.TELEPORT_SERVER_WARP),
                                            onSuccess = {
                                                redis.publish(PlayerRedirectRequest(player.uuid, serverType))
                                            })
                                    }
                                }

                                opener.sendMessage(
                                    Component.text(
                                        "Teleporting you to the ${warp.name.lowercase()} warp...",
                                        NamedTextColor.GREEN
                                    )
                                )
                            }
                        }
                    })
        }
    }

    private fun getLore(warp: WarpType): List<String> {
        return mutableListOf<String>().apply {
            add(" ")
            addAll(warp.description)
            add(" ")
            add("&7&oClick to teleport!")
            add(" ")
        }
    }
}