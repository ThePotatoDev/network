package gg.tater.shared.player.position

import gg.tater.shared.network.model.server.ServerType
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.redis.Redis
import org.bukkit.Location
import java.util.concurrent.CompletableFuture

abstract class PlayerPositionResolver(val redis: Redis) {

    abstract fun getLocation(
        data: PlayerDataModel,
        type: ServerType
    ): CompletableFuture<Location?>

    enum class Type {
        TELEPORT_SPAWN,
        TELEPORT_ISLAND_VISIT,
        TELEPORT_ISLAND_HOME,
        TELEPORT_ISLAND_WARP,
        TELEPORT_PLAYER_SHOP,
        TELEPORT_SERVER_WARP,
        NONE
    }
}