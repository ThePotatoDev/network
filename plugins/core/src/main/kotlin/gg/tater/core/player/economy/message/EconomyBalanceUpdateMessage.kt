package gg.tater.core.player.economy.message

import com.google.gson.*
import gg.tater.core.JsonAdapter
import gg.tater.core.annotation.Mapping
import gg.tater.core.annotation.Message
import java.lang.reflect.Type
import java.util.*

@Mapping("economy_balance_update_message")
@Message("economy_balance_updates")
class EconomyBalanceUpdateMessage(val uuid: UUID, val newBalance: Double) {

    private companion object {
        const val UUID_FIELD = "uuid"
        const val NEW_BALANCE_FIELD = "new_balance"
    }

    @JsonAdapter(EconomyBalanceUpdateMessage::class)
    class Adapter : JsonSerializer<EconomyBalanceUpdateMessage>, JsonDeserializer<EconomyBalanceUpdateMessage> {
        override fun serialize(
            message: EconomyBalanceUpdateMessage,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {
                addProperty(UUID_FIELD, message.uuid.toString())
                addProperty(NEW_BALANCE_FIELD, message.newBalance)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): EconomyBalanceUpdateMessage {
            return (element as JsonObject).let {
                val uuid = UUID.fromString(it.get(UUID_FIELD).asString)
                val newBalance = it.get(NEW_BALANCE_FIELD).asDouble
                EconomyBalanceUpdateMessage(uuid, newBalance)
            }
        }
    }
}