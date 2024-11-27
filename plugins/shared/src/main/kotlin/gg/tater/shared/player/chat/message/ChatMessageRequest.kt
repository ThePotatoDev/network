package gg.tater.shared.player.chat.message

import com.google.gson.*
import gg.tater.shared.redis.Redis
import java.lang.reflect.Type
import java.util.*

@Redis.Mapping("chat_message_req")
@Redis.ReqRes("chat_message")
class ChatMessageRequest(
    val targets: MutableSet<UUID>?,
    var permission: String?,
    val parts: MutableMap<ChatMessagePart, String> = mutableMapOf()
) {

    companion object {
        private const val TARGETS_FIELD = "targets"
        private const val PERMISSION_FIELD = "permission"
        private const val PARTS_FIELD = "parts"
    }

    fun hasSpecialColor(): Boolean {
        return parts[ChatMessagePart.START_CHAT_COLOR] != null && parts[ChatMessagePart.END_CHAT_COLOR] != null
    }

    fun getPart(part: ChatMessagePart): String {
        return parts[part]!!
    }

    fun setPart(part: ChatMessagePart, value: String) {
        parts[part] = value
    }

    class Adapter : JsonSerializer<ChatMessageRequest>, JsonDeserializer<ChatMessageRequest> {
        override fun serialize(
            model: ChatMessageRequest,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {
                val parts = JsonObject()

                if (model.targets != null) {
                    add(TARGETS_FIELD, JsonArray().apply {
                        model.targets.forEach { add(it.toString()) }
                    })
                }

                model.permission?.let { addProperty(PERMISSION_FIELD, it) }

                for (part in model.parts) {
                    parts.addProperty(part.key.name, part.value)
                }

                add(PARTS_FIELD, parts)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): ChatMessageRequest {
            return (element as JsonObject).run {
                val parts: MutableMap<ChatMessagePart, String> = mutableMapOf()
                var targets: MutableSet<UUID>? = null

                if (has(TARGETS_FIELD)) {
                    targets = mutableSetOf()
                    get(TARGETS_FIELD).asJsonArray.forEach { targets.add(UUID.fromString(it.asString)) }
                }

                get(PARTS_FIELD).asJsonObject.entrySet()
                    .forEach { parts[ChatMessagePart.valueOf(it.key)] = it.value.asString }

                val permission = get(PERMISSION_FIELD)?.asString
                val request = ChatMessageRequest(targets, permission, parts)

                request
            }
        }
    }
}