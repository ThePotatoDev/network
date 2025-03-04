package gg.tater.core.player.event

import gg.tater.core.island.player.IslandPlayer
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.*

class PlayerPlacementEvent<K : IslandPlayer>(val player: K, val islandId: UUID) : Event() {

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