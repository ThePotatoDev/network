package gg.tater.shared.player.warp

import gg.tater.shared.network.model.server.ServerType
import gg.tater.shared.player.PlayerRedirectRequest
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.redis.Redis
import me.lucko.helper.menu.Gui
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class WarpGui(private val opener: Player, private val redis: Redis, private val server: String) :
    Gui(opener, 3, "Server Warps") {

    private companion object {

    }

    override fun redraw() {
        for (warp in WarpType.entries) {
            val location = Location(
                Bukkit.getWorld("world"),
                warp.position.x,
                warp.position.y,
                warp.position.z
            )

            setItem(warp.slot, warp.icon.build {
                redis.players().getAsync(opener.uniqueId).thenAcceptAsync { player ->

                    // If the player is already on the warp's server
                    if (player.currentServerId == server) {
                        opener.teleportAsync(
                            location
                        )
                        return@thenAcceptAsync
                    }

                    player.setSpawn(ServerType.SPAWN, warp.position)
                    redis.players().fastPut(
                        player.uuid,
                        player.setPositionResolver(PlayerPositionResolver.Type.TELEPORT_SERVER_WARP)
                    )
                    redis.publish(PlayerRedirectRequest(player.uuid, ServerType.SPAWN))
                }
            })
        }
    }
}