package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.island.IslandService
import gg.tater.shared.island.setting.IslandSettingGui
import gg.tater.shared.player.PlayerService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandSettingSubCommand(
    private val redis: Redis,
    private val players: PlayerService = Services.load(PlayerService::class.java),
    private val islands: IslandService = Services.load(IslandService::class.java)
) : IslandSubCommand {

    override fun id(): String {
        return "setting"
    }

    override fun handle(context: CommandContext<Player>) {
        val sender = context.sender()

        players.get(sender.uniqueId).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            IslandSettingGui(sender, redis, island).open()
        }
    }
}