package gg.tater.shared.island.command

import gg.tater.shared.island.Island
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

interface IslandSubCommand<T: Island> {

    fun id(): String

    fun handle(context: CommandContext<Player>)

}