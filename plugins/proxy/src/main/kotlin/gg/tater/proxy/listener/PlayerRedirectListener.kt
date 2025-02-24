package gg.tater.proxy.listener

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import gg.tater.shared.player.PlayerRedirectRequest
import gg.tater.shared.redis.Redis

class PlayerRedirectListener(private val proxy: ProxyServer, private val redis: Redis) {

    fun exec() {
        redis.listen<PlayerRedirectRequest> {
            val player = proxy.getPlayer(it.uuid).orElse(null) ?: return@listen
            val target: RegisteredServer?

            // If the direct server exists, move them there instead
            val serverId = it.server
            if (serverId != null) {
                target = proxy.getServer(serverId).orElse(null)
            } else {
                val id = redis.getReadyServer(it.type).id
                target = proxy.getServer(id).orElse(null)
            }

            if (target == null) return@listen
            player.createConnectionRequest(target).fireAndForget()
        }
    }
}