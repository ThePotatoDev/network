package gg.tater.shared.player.trade

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.gson.*
import me.lucko.helper.serialize.Serializers
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type
import java.util.*

class TradeEntry(
    val sender: UUID,
    val target: UUID,
    private val offerings: Multimap<UUID, ItemStack> = ArrayListMultimap.create()
) {

    private companion object {
        const val SENDER_FIELD = "sender"
        const val TARGET_FIELD = "target"
        const val OFFERINGS_FIELD = "offerings"
    }

    fun getOfferingsFor(uuid: UUID): List<ItemStack> {
        return offerings.get(uuid).toList()
    }

    fun addOffering(uuid: UUID, stack: ItemStack): Boolean {
        return offerings.put(uuid, stack)
    }

    class Adapter : JsonSerializer<TradeEntry>, JsonDeserializer<TradeEntry> {
        override fun serialize(entry: TradeEntry, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(SENDER_FIELD, entry.sender.toString())
                addProperty(TARGET_FIELD, entry.target.toString())
                add(OFFERINGS_FIELD, JsonObject().apply {
                    entry.offerings.forEach { uuid, stack ->
                        this.add(uuid.toString(), Serializers.serializeItemstack(stack))
                    }
                })
            }
        }

        override fun deserialize(element: JsonElement, type: Type, context: JsonDeserializationContext): TradeEntry {
            return element.asJsonObject.let {
                val senderId = UUID.fromString(it.get(SENDER_FIELD).asString)
                val targetId = UUID.fromString(it.get(TARGET_FIELD).asString)

                TradeEntry(senderId, targetId).apply {
                    for (offering in it.get(OFFERINGS_FIELD).asJsonObject.entrySet()) {
                        val uuid = UUID.fromString(offering.key)
                        val stack = Serializers.deserializeItemstack(offering.value)
                        this.addOffering(uuid, stack)
                    }
                }
            }
        }
    }
}