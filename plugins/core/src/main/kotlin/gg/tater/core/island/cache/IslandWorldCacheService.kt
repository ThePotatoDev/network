package gg.tater.core.island.cache

import gg.tater.core.UUID_REGEX
import gg.tater.core.island.Island
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.World
import java.util.*

interface IslandWorldCacheService<T : Island> : TerminableModule {

    fun getIsland(world: World): T?

    fun refresh(id: String)

    fun invalidate(id: String)
}

fun World.isIslandWorld(): Boolean {
    return UUID_REGEX.matches(this.name)
}

fun World.toIslandId(): UUID? {
    if (!isIslandWorld()) return null
    return UUID.fromString(this.name)
}