package gg.tater.core.player.kit.model

import org.bukkit.inventory.ItemStack

data class KitDataModel(
    val slot: Int,
    val name: String,
    val friendly: String,
    val permission: String,
    val cooldown: Long,
    val contents: List<ItemStack>,
    val icon: ItemStack,
    val default: Boolean
) {

    override fun equals(other: Any?): Boolean {
        return other is KitDataModel && other.name.equals(this.name, true)
    }
}