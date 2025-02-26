package gg.tater.proxy.command

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import gg.tater.shared.server.model.ServerType
import gg.tater.shared.redis.Redis
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class HubCommand(private val redis: Redis, private val proxy: ProxyServer) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()

        if (source is ConsoleCommandSource) {
            source.sendMessage(Component.text("Console cannot use this command.", NamedTextColor.RED))
            return
        }

        val player = source as Player
        val currentServer = player.currentServer.orElse(null)
        if (currentServer != null && currentServer.serverInfo.name.contains("hub")) {
            player.sendMessage(Component.text("You are already connected to a hub!", NamedTextColor.RED))
            return
        }

        redis.getReadyServerAsync(ServerType.HUB).thenAccept { server ->
            if (server == null) {
                source.sendMessage(Component.text("Error finding hub server to direct you to.", NamedTextColor.RED))
                return@thenAccept
            }

            val registeredServer = proxy.getServer(server.id).orElse(null)
            if (registeredServer == null) {
                player.sendMessage(Component.text("Error finding hub server through the proxy.", NamedTextColor.RED))
                return@thenAccept
            }

            player.createConnectionRequest(registeredServer).fireAndForget()
            player.sendMessage(Component.text("Connecting you to a hub...", NamedTextColor.GREEN))
        }
    }
}