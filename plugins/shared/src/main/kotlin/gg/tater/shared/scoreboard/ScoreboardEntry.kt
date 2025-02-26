package gg.tater.shared.scoreboard

import gg.tater.shared.server.model.ServerType
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.entity.Player

interface ScoreboardEntry: TerminableModule {

    fun id(): String

    fun display(player: Player)

    fun applicableTo(): Set<ServerType>

}