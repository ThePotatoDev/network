package gg.tater.oneblock.island.subcommand

import gg.tater.oneblock.island.OneBlockIsland
import gg.tater.core.island.command.IslandSubCommand
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class OneBlockPhasesSubCommand : IslandSubCommand<OneBlockIsland> {

    override fun id(): String {
        return "phases"
    }

    override fun handle(context: CommandContext<Player>) {
        val sender = context.sender()
    }
}