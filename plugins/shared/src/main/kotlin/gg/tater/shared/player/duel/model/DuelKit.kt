package gg.tater.shared.player.duel.model

import org.bukkit.inventory.ItemStack

interface DuelKit {

    companion object {
        private val KITS: MutableSet<DuelKit> = mutableSetOf()

        fun getKit(id: String): DuelKit? {
            return KITS.firstOrNull { it.id().equals(id, true) }
        }
    }

    fun id(): String

    fun armor(): Array<ItemStack>

    fun contents(): Array<ItemStack>
}