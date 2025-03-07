package gg.tater.oneblock.event

import gg.tater.oneblock.island.OneBlockIsland
import gg.tater.oneblock.island.phase.model.OneBlockPhase
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class OneBlockPhaseCompleteEvent(val island: OneBlockIsland, val phase: OneBlockPhase) : Event() {

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