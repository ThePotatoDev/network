package gg.tater.core.island.command

import gg.tater.core.island.Island
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

interface IslandSubCommand<T: Island> {

    fun id(): String

    fun handle(context: CommandContext<Player>)

}