package gg.tater.proxy.listener

import com.velocitypowered.api.proxy.ProxyServer
import gg.tater.core.island.message.placement.IslandPlacementResponse
import gg.tater.core.redis.Redis

class IslandPlacementListener(private val proxy: ProxyServer, private val redis: Redis) {

    fun exec() {
        redis.listen<IslandPlacementResponse> {
            val player = proxy.getPlayer(it.playerId).orElse(null) ?: return@listen
            val server = proxy.getServer(it.server).orElse(null) ?: return@listen
            if (it.internal) {
                player.createConnectionRequest(server).fireAndForget()
            }
        }
    }
}