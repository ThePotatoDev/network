package gg.tater.oneblock.island.phase.gui

import gg.tater.oneblock.island.phase.model.OneBlockPhaseSerivce
import me.lucko.helper.Services
import me.lucko.helper.menu.Gui
import me.lucko.helper.menu.scheme.MenuScheme
import me.lucko.helper.menu.scheme.StandardSchemeMappings
import org.bukkit.entity.Player

class OneBlockPhaseGui(player: Player) : Gui(player, 4, "OneBlock Phases") {

    private companion object {
        val ITEM_SCHEME: MenuScheme = MenuScheme(StandardSchemeMappings.EMPTY)
            .mask("000000000")
            .mask("011111110")
            .mask("011111110")
            .mask("000000000")
    }

    private val phases = Services.load(OneBlockPhaseSerivce::class.java)

    override fun redraw() {
        val populator = ITEM_SCHEME.newPopulator(this)

        for (phase in phases.all().sortedByDescending { it.id }) {
            populator.accept(phase.icon.build {

            })
        }
    }
}