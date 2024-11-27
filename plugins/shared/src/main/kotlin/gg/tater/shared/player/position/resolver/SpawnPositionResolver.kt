package gg.tater.shared.player.position.resolver

import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.redis.Redis
import gg.tater.shared.network.model.ServerType
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.concurrent.CompletableFuture

class SpawnPositionResolver(redis: Redis) : PlayerPositionResolver(redis) {

    override fun getLocation(
        data: PlayerDataModel,
        type: ServerType
    ): CompletableFuture<Location?> {
        val spawn = data.getSpawn(type)
        return CompletableFuture.completedFuture(
            Location(
                Bukkit.getWorld("world")!!,
                spawn.x,
                spawn.y,
                spawn.z,
                spawn.yaw,
                spawn.pitch
            )
        )
    }
}