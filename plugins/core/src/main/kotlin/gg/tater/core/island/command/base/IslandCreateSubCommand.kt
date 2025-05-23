package gg.tater.core.island.command.base

import gg.tater.core.island.Island
import gg.tater.core.island.IslandService
import gg.tater.core.island.command.IslandSubCommand
import gg.tater.core.island.player.IslandPlayer
import gg.tater.core.island.player.IslandPlayerService
import gg.tater.core.redis.Redis
import gg.tater.core.server.model.ServerType
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandCreateSubCommand<T : Island, K : IslandPlayer>(val islandServerType: ServerType) : IslandSubCommand<T> {

    override fun id(): String {
        return "create"
    }

    override fun handle(context: CommandContext<Player>) {
        val redis = Services.load(Redis::class.java)
        val players: IslandPlayerService<K> = Services.load(IslandPlayerService::class.java) as IslandPlayerService<K>
        val islands: IslandService<T, K> =
            Services.load(IslandService::class.java) as IslandService<T, K>

        val sender = context.sender()
        val uuid = sender.uniqueId

        players.get(uuid).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
            if (island != null) {
                context.reply("&cYou already have an island.")
                return@thenAcceptAsync
            }

            val server = redis.getServer(islandServerType)
            if (server == null) {
                context.reply("&cCould not find server.")
                return@thenAcceptAsync
            }

            context.reply("&7&oCreating your OneBlock...")
            islands.createFor(player, server)
        }
    }
}