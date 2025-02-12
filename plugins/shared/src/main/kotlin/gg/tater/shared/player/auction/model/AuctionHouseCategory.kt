package gg.tater.shared.player.auction.model

import java.util.function.Predicate

enum class AuctionHouseCategory(val friendly: String, val validator: Predicate<AuctionHouseItem>?) {

    ALL("All", null),

    SPAWNERS("Spawners", Predicate { item -> item.stack.type.name.contains("SPAWNER") }),

    CRATE_KEYS("Crate Keys", null),

    ENCHANTED_BOOKS("Enchanted Books", Predicate { item -> item.stack.type.name.contains("ENCHANTED_BOOK") }),

    REDSTONE(
        "Redstone",
        Predicate { item -> item.stack.type.creativeCategory == org.bukkit.inventory.CreativeCategory.REDSTONE }),

    FOOD("Food", Predicate { item -> item.stack.type.creativeCategory == org.bukkit.inventory.CreativeCategory.FOOD }),

    COMBAT(
        "Combat",
        Predicate { item -> item.stack.type.creativeCategory == org.bukkit.inventory.CreativeCategory.COMBAT }),

    TOOLS(
        "Tools",
        Predicate { item -> item.stack.type.creativeCategory == org.bukkit.inventory.CreativeCategory.TOOLS }),

    MISC(
        "Misc",
        Predicate { item -> item.stack.type.creativeCategory == org.bukkit.inventory.CreativeCategory.MISC }),

    BREWING(
        "Brewing",
        Predicate { item -> item.stack.type.creativeCategory == org.bukkit.inventory.CreativeCategory.BREWING }),

    BUILDING_BLOCKS(
        "Building Blocks",
        Predicate { item -> item.stack.type.creativeCategory == org.bukkit.inventory.CreativeCategory.BUILDING_BLOCKS }),

    DECORATIONS(
        "Decorations",
        Predicate { item -> item.stack.type.creativeCategory == org.bukkit.inventory.CreativeCategory.DECORATIONS }),

    TRANSPORTATION(
        "Transportation",
        Predicate { item -> item.stack.type.creativeCategory == org.bukkit.inventory.CreativeCategory.TRANSPORTATION }),

    CHAT_TAGS("Chat Tags", null)
    ;

    fun next(): AuctionHouseCategory {
        return entries[(this.ordinal + 1) % entries.size]
    }
}