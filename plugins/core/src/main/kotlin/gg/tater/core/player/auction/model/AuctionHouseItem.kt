package gg.tater.core.player.auction.model

import com.google.gson.*
import gg.tater.core.Json
import gg.tater.core.JsonAdapter
import gg.tater.core.Mapping
import me.lucko.helper.serialize.Serializers
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type
import java.time.Instant
import java.util.*

@Mapping("auction_house_item")
class AuctionHouseItem(
    val id: UUID = UUID.randomUUID(),
    val ownerId: UUID,
    val ownerName: String,
    val stack: ItemStack,
    val price: Double,
    val listedAt: Long
) {

    companion object {
        private const val ID_FIELD = "id"
        private const val OWNER_ID_FIELD = "owner_id"
        private const val OWNER_NAME_FIELD = "owner_name"
        private const val STACK_FIELD = "item"
        private const val PRICE_FIELD = "price"
        private const val LISTED_AT_FIELD = "listed_at"
    }

    constructor(player: Player, price: Double) : this(
        UUID.randomUUID(),
        player.uniqueId,
        player.name,
        player.inventory.itemInMainHand,
        price,
        Instant.now().toEpochMilli()
    )

    override fun equals(other: Any?): Boolean {
        return other is AuctionHouseItem && other.id == this.id
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    @JsonAdapter(AuctionHouseItem::class)
    class Adapter : JsonSerializer<AuctionHouseItem>, JsonDeserializer<AuctionHouseItem> {
        override fun serialize(item: AuctionHouseItem, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(ID_FIELD, item.id.toString())
                addProperty(OWNER_ID_FIELD, item.ownerId.toString())
                addProperty(OWNER_NAME_FIELD, item.ownerName)
                add(STACK_FIELD, Serializers.serializeItemstack(item.stack))
                addProperty(PRICE_FIELD, item.price)
                addProperty(LISTED_AT_FIELD, item.listedAt)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): AuctionHouseItem {
            (element as JsonObject).let {
                val id = UUID.fromString(it.get(ID_FIELD).asString)
                val ownerId = UUID.fromString(it.get(OWNER_ID_FIELD).asString)
                val ownerName = it.get(OWNER_NAME_FIELD).asString
                val stack = Serializers.deserializeItemstack(it.get(STACK_FIELD).asJsonPrimitive)
                val price = it.get(PRICE_FIELD).asDouble
                val listedAt = it.get(LISTED_AT_FIELD).asLong
                return AuctionHouseItem(id, ownerId, ownerName, stack, price, listedAt)
            }
        }
    }
}