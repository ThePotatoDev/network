package gg.tater.shared.player.teleport.message

import com.google.gson.*
import gg.tater.shared.Json
import gg.tater.shared.player.teleport.TeleportRequest
import gg.tater.shared.redis.Redis
import java.lang.reflect.Type

@Redis.Mapping("chat_message_req")
@Redis.ReqRes("chat_message")
class TeleportRequestMessage(val request: TeleportRequest) {

    private companion object {
        const val REQUEST_FIELD = "request"
    }

    class Adapter : JsonSerializer<TeleportRequestMessage>, JsonDeserializer<TeleportRequestMessage> {
        override fun serialize(
            message: TeleportRequestMessage,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {
                addProperty(REQUEST_FIELD, Json.INSTANCE.toJson(message.request))
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): TeleportRequestMessage {
            return TeleportRequestMessage(
                Json.INSTANCE.fromJson(
                    (element as JsonObject).get(REQUEST_FIELD).asString,
                    TeleportRequest::class.java
                )
            )
        }
    }
}