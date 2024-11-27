package gg.tater.shared.island

import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.World

interface IslandService : TerminableModule {

    fun getIsland(world: World): Island?

}