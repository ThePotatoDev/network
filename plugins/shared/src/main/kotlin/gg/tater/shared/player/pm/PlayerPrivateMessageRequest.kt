package gg.tater.shared.player.pm

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import gg.tater.shared.annotation.Message
import gg.tater.shared.redis.Redis
import java.lang.reflect.Type
import java.util.*

@Mapping("player_private_message_req")
@Message("player_private_messages")
class PlayerPrivateMessageRequest(
    val senderName: String,
    val senderId: UUID,
    val targetName: String,
    val targetId: UUID,
    val message: String,
) {

    companion object {
        const val SENDER_NAME_FIELD = "sender_name"
        const val SENDER_UUID_FIELD = "sender_uuid"
        const val TARGET_NAME_FIELD = "target_name"
        const val TARGET_UUID_FIELD = "target_uuid"
        const val MESSAGE_FIELD = "message"
    }

    @JsonAdapter(PlayerPrivateMessageRequest::class)
    class Adapter : JsonSerializer<PlayerPrivateMessageRequest>, JsonDeserializer<PlayerPrivateMessageRequest> {
        override fun serialize(
            model: PlayerPrivateMessageRequest,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val json = JsonObject()
            json.addProperty(SENDER_UUID_FIELD, model.senderId.toString())
            json.addProperty(SENDER_NAME_FIELD, model.senderName)
            json.addProperty(TARGET_NAME_FIELD, model.targetName)
            json.addProperty(TARGET_UUID_FIELD, model.targetId.toString())
            json.addProperty(MESSAGE_FIELD, model.message)
            return json
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): PlayerPrivateMessageRequest {
            val json = element as JsonObject
            val senderId = UUID.fromString(json.get(SENDER_UUID_FIELD).asString)
            val senderName = json.get(SENDER_NAME_FIELD).asString
            val targetName = json.get(TARGET_NAME_FIELD).asString
            val message = json.get(MESSAGE_FIELD).asString
            val targetId = UUID.fromString(json.get(TARGET_UUID_FIELD).asString)
            return PlayerPrivateMessageRequest(senderName, senderId, targetName, targetId, message)
        }
    }
}