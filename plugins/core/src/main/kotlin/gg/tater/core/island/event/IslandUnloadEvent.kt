package gg.tater.core.island.event

import com.infernalsuite.aswm.api.world.SlimeWorld
import gg.tater.core.island.Island
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event fires when an island has unloaded
 * on the server instance the island is active on.
 */
class IslandUnloadEvent<T : Island>(val island: T, val world: SlimeWorld) : Event() {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}