package gg.tater.oneblock.event

import gg.tater.oneblock.island.OneBlockIsland
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class OneBlockMineEvent(
    val player: Player,
    val island: OneBlockIsland,
    val block: Block,
    var nextMaterialType: Material? = null,
    var handled: Boolean = false
) :
    Event(), Cancellable {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    private var cancelled: Boolean = false

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    override fun isCancelled(): Boolean {
       return cancelled
    }

    override fun setCancelled(value: Boolean) {
        this.cancelled = value
    }
}