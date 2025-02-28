package gg.tater.core.player.teleport.message

import com.google.gson.*
import gg.tater.core.Json
import gg.tater.core.annotation.Mapping
import gg.tater.core.annotation.Message
import gg.tater.core.player.teleport.TeleportRequest
import java.lang.reflect.Type

@Mapping("teleport_req_message")
@Message("teleport_req_messages")
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