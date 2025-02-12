package gg.tater.shared.player.auction.model

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

enum class AuctionHouseSort(val friendly: String, val comparator: Comparator<AuctionHouseItem>) {

    DATE_POSTED("Date Posted", Comparator.comparingLong { item -> item.listedAt }),

    ALPHABETICAL(
        "Alphabetical",
        Comparator.comparing { item ->
            LegacyComponentSerializer.legacyAmpersand().serialize(item.stack.displayName())
        }),

    PRICE("Price", Comparator.comparingDouble { item -> item.price })
    ;

    fun next(): AuctionHouseSort {
        return entries[(this.ordinal + 1) % entries.size]
    }
}