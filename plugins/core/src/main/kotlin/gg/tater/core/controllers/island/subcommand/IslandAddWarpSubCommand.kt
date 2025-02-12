package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.island.IslandService
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.position.WrappedPosition
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandAddWarpSubCommand(
    private val redis: Redis,
    private val players: PlayerService = Services.load(PlayerService::class.java),
    private val islands: IslandService = Services.load(IslandService::class.java)
) : IslandSubCommand {

    override fun id(): String {
        return "addwarp"
    }

    override fun handle(context: CommandContext<Player>) {
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