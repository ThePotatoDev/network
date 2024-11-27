package gg.tater.shared.player.position.resolver

import gg.tater.shared.redis.Redis
import gg.tater.shared.network.model.ServerType
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.player.position.PlayerPositionResolver
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.concurrent.CompletableFuture

class IslandWarpPositionResolver(redis: Redis) : PlayerPositionResolver(redis) {

    override fun getLocation(
        data: PlayerDataModel,
        type: ServerType
    ): CompletableFuture<Location?> {
        val warp = data.getSpawn(type)
        return CompletableFuture.completedFuture(
            Location(
                Bukkit.getWorld(data.islandId.toString()),
                warp.x,
                warp.y,
                warp.z,
                warp.yaw,
                warp.pitch
            )
        )
    }
}