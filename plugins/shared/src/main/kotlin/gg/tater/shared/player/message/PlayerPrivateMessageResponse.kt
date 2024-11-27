package gg.tater.shared.player.message

import com.google.gson.*
import gg.tater.shared.redis.Redis
import java.lang.reflect.Type
import java.util.*

@Redis.Mapping("player_private_message_resp")
@Redis.ReqRes("player_private_messages")
class PlayerPrivateMessageResponse(
    val senderName: String,
    val senderId: UUID,
    val targetName: String,
    val targetId: UUID,
    val message: String
) {

    companion object {
        const val SENDER_NAME_FIELD = "sender_name"
        const val SENDER_UUID_FIELD = "sender_uuid"
        const val TARGET_NAME_FIELD = "target_name"
        const val TARGET_UUID_FIELD = "target_uuid"
        const val MESSAGE_FIELD = "message"
    }

    constructor(request: PlayerPrivateMessageRequest) : this(
        request.senderName,
        request.senderId,
        request.targetName,
        request.targetId,
        request.message
    )

    class Adapter : JsonSerializer<PlayerPrivateMessageResponse>, JsonDeserializer<PlayerPrivateMessageResponse> {
        override fun serialize(
            model: PlayerPrivateMessageResponse,
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
        ): PlayerPrivateMessageResponse {
            val json = element as JsonObject
            val senderId = UUID.fromString(json.get(SENDER_UUID_FIELD).asString)
            val senderName = json.get(SENDER_NAME_FIELD).asString
            val targetName = json.get(TARGET_NAME_FIELD).asString
            val targetId = UUID.fromString(json.get(TARGET_UUID_FIELD).asString)
            val message = json.get(MESSAGE_FIELD).asString
            return PlayerPrivateMessageResponse(senderName, senderId, targetName, targetId, message)
        }
    }
}