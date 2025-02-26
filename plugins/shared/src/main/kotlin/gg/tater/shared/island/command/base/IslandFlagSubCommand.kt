package gg.tater.shared.island.command.base

import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.command.IslandSubCommand
import gg.tater.shared.island.flag.IslandFlagGui
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandFlagSubCommand<T : Island> : IslandSubCommand<T> {

    override fun id(): String {
        return "flags"
    }

    override fun handle(context: CommandContext<Player>) {
        val sender = context.sender()
        val players: PlayerService = Services.load(PlayerService::class.java)
        val islands: IslandService<T> =
            Services.load(IslandService::class.java) as IslandService<T>

        players.get(sender.uniqueId).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            IslandFlagGui(sender, island).open()
        }
    }
}