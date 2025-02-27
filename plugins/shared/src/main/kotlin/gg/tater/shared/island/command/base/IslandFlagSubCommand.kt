package gg.tater.shared.island.command.base

import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.command.IslandSubCommand
import gg.tater.shared.island.flag.IslandFlagGui
import gg.tater.shared.island.player.IslandPlayer
import gg.tater.shared.island.player.IslandPlayerService
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandFlagSubCommand<T : Island, K : IslandPlayer> : IslandSubCommand<T> {

    override fun id(): String {
        return "flags"
    }

    override fun handle(context: CommandContext<Player>) {
        val sender = context.sender()
        val players: IslandPlayerService<K> = Services.load(IslandPlayerService::class.java) as IslandPlayerService<K>
        val islands: IslandService<T, K> =
            Services.load(IslandService::class.java) as IslandService<T, K>

        players.get(sender.uniqueId).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            IslandFlagGui<T, K>(sender, island).open()
        }
    }
}