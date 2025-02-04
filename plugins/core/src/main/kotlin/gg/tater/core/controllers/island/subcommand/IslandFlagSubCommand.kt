package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.redis.Redis
import gg.tater.shared.island.flag.IslandFlagGui
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandFlagSubCommand(private val redis: Redis) : IslandSubCommand {

    override fun id(): String {
        return "flags"
    }

    override fun handle(context: CommandContext<Player>) {
        val sender = context.sender()

        redis.players().getAsync(sender.uniqueId).thenAcceptAsync { player ->
            val island = player.islandId?.let { redis.islands()[it] }
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            IslandFlagGui(sender, island, redis).open()
        }
    }
}