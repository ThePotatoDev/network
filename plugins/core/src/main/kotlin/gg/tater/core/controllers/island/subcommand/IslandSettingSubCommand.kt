package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.redis.Redis
import gg.tater.shared.island.setting.IslandSettingGui
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandSettingSubCommand(val redis: Redis) : IslandSubCommand {

    override fun id(): String {
        return "setting"
    }

    override fun handle(context: CommandContext<Player>) {
        val sender = context.sender()

        redis.players().getAsync(sender.uniqueId).thenAcceptAsync { player ->
            val island = player.islandId?.let { redis.islands()[it] }
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            IslandSettingGui(sender, redis, island).open()
        }
    }
}