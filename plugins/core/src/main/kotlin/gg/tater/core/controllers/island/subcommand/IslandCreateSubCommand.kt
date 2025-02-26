package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.island.IslandService
import gg.tater.shared.server.model.ServerType
import gg.tater.shared.player.PlayerService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandCreateSubCommand(private val redis: Redis) : IslandSubCommand {

    override fun id(): String {
        return "create"
    }

    override fun handle(context: CommandContext<Player>) {
        val players: PlayerService = Services.load(PlayerService::class.java)
        val islands: IslandService = Services.load(IslandService::class.java)

        val sender = context.sender()
        val uuid = sender.uniqueId

        players.get(uuid).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
            if (island != null) {
                context.reply("&cYou already have an island.")
                return@thenAcceptAsync
            }

            val server = redis.getServer(ServerType.SERVER)
            if (server == null) {
                context.reply("&cCould not find server.")
                return@thenAcceptAsync
            }

            context.reply("&7&oCreating your OneBlock...")
            islands.createFor(player, server)
        }
    }
}