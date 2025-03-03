package gg.tater.oneblock.event

import gg.tater.oneblock.island.OneBlockIsland
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class OneBlockMineEvent(val player: Player, val island: OneBlockIsland) : Event() {

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