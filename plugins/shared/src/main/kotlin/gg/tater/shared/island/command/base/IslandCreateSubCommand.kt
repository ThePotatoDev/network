package gg.tater.shared.island.command.base

import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.command.IslandSubCommand
import gg.tater.shared.player.PlayerService
import gg.tater.shared.redis.Redis
import gg.tater.shared.server.model.ServerType
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandCreateSubCommand<T : Island> : IslandSubCommand<T> {

    override fun id(): String {
        return "create"
    }

    override fun handle(context: CommandContext<Player>) {
        val redis = Services.load(Redis::class.java)
        val players: PlayerService = Services.load(PlayerService::class.java)
        val islands: IslandService<T> =
            Services.load(IslandService::class.java) as IslandService<T>

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