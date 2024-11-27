package gg.tater.shared.player.progression.crafting

data class CraftingItem(val name: String)

class CraftingItemRegistry {
    companion object {
        private val items: MutableSet<CraftingItem> = mutableSetOf()

        fun registerItem(item: CraftingItem) {
            items.add(item)
        }

        fun unregisterItem(item: CraftingItem) {
            items.remove(item)
        }
    }
}