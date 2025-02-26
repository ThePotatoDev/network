package gg.tater.shared.island.cache

import gg.tater.shared.island.Island
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.World

interface IslandWorldCacheService<T : Island> : TerminableModule {

    fun getIsland(world: World): T?

    fun refresh(id: String)

    fun invalidate(id: String)
}