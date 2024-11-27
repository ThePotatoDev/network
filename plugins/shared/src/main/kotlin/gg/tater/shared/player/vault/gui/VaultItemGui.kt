package gg.tater.shared.player.vault.gui

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class VaultItemGui(val player: Player, val id: Int, stacks: Array<out ItemStack?>) : InventoryHolder {

    private val handle: Inventory = Bukkit.createInventory(this, 54, Component.text("Vault #${id + 1}"))

    init {
        for (stack in stacks) {
            if (stack == null) continue
            handle.addItem(stack)
        }
    }

    fun open() {
        player.openInventory(handle)
    }

    override fun getInventory(): Inventory {
        return handle
    }
}