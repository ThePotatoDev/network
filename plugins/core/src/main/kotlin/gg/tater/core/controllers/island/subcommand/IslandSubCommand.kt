package gg.tater.core.controllers.island.subcommand

import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

interface IslandSubCommand {

    fun id(): String

    fun handle(context: CommandContext<Player>)

}