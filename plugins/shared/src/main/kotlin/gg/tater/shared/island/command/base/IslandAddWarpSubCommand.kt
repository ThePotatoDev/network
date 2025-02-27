package gg.tater.shared.island.command.base

import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.command.IslandSubCommand
import gg.tater.shared.island.player.IslandPlayer
import gg.tater.shared.island.player.IslandPlayerService
import gg.tater.shared.position.WrappedPosition
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandAddWarpSubCommand<T : Island, K : IslandPlayer> : IslandSubCommand<T> {

    override fun id(): String {
        return "addwarp"
    }

    override fun handle(context: CommandContext<Player>) {
        val players: IslandPlayerService<K> = Services.load(IslandPlayerService::class.java) as IslandPlayerService<K>
        val islands: IslandService<T, K> =
            Services.load(IslandService::class.java) as IslandService<T, K>

        if (context.args().size != 2) {
            context.reply("&cUsage: /is addwarp <name>")
            return
        }

        val name = context.arg(1).parseOrFail(String::class.java)

        val sender = context.sender()
        val uuid = sender.uniqueId

        players.get(uuid).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            if (island.warps.any { it.key.equals(name, true) }) {
                context.reply("&cAn island warp with that name already exists!")
                return@thenAcceptAsync
            }

            island.warps[name] = WrappedPosition(sender.location)
            islands.save(island)

            context.reply("&aCreated island warp: $name")
        }
    }
}