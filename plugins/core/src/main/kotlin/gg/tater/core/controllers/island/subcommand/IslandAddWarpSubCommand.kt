package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.redis.Redis
import gg.tater.shared.player.position.WrappedPosition
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandAddWarpSubCommand(private val redis: Redis) : IslandSubCommand {

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

        redis.players().getAsync(uuid).thenAcceptAsync { data ->
            val island = data.islandId?.let { redis.islands()[it] }
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            if (island.warps.any { it.key.equals(name, true) }) {
                context.reply("&cAn island warp with that name already exists!")
                return@thenAcceptAsync
            }

            island.warps[name] = WrappedPosition(sender.location)
            redis.islands().fastPutAsync(island.id, island)

            context.reply("&aCreated island warp: $name")
        }
    }
}