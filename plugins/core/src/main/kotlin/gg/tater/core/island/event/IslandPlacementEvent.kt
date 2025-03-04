package gg.tater.core.island.event

import gg.tater.core.island.Island
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event fires when an island has completed placement
 * on the server instance the island is active on.
 */
class IslandPlacementEvent<T : Island>(val island: T) : Event() {

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