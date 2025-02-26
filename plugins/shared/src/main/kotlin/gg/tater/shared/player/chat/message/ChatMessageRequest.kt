package gg.tater.shared.player.chat.message

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import gg.tater.shared.annotation.Message
import gg.tater.shared.network.server.ServerType
import java.lang.reflect.Type
import java.util.*

@Mapping("chat_message_req")
@Message("chat_messages")
class ChatMessageRequest(
    val targetPlayers: MutableSet<UUID>?,
    var permission: String?,
    val targetServers: Set<ServerType>,
    val parts: MutableMap<ChatMessagePart, String> = mutableMapOf(),
) {

    companion object {
        private const val TARGET_PLAYERS_FIELD = "target_players"
        private const val PERMISSION_FIELD = "permission"
        private const val PARTS_FIELD = "parts"
        private const val TARGET_SERVERS_FIELD = "target_servers"
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

    @JsonAdapter(ChatMessageRequest::class)
    class Adapter : JsonSerializer<ChatMessageRequest>, JsonDeserializer<ChatMessageRequest> {
        override fun serialize(
            model: ChatMessageRequest,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {
                val parts = JsonObject()

                if (model.targetPlayers != null) {
                    add(TARGET_PLAYERS_FIELD, JsonArray().apply {
                        model.targetPlayers.forEach { add(it.toString()) }
                    })
                }

                add(TARGET_SERVERS_FIELD, JsonArray().apply {
                    model.targetServers.forEach { add(it.name) }
                })

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
                val targetServers: MutableSet<ServerType> = mutableSetOf()
                var targetPlayers: MutableSet<UUID>? = null

                if (has(TARGET_PLAYERS_FIELD)) {
                    targetPlayers = mutableSetOf()
                    get(TARGET_PLAYERS_FIELD).asJsonArray.forEach { targetPlayers.add(UUID.fromString(it.asString)) }
                }

                get(TARGET_SERVERS_FIELD).asJsonArray.forEach { targetServers.add(ServerType.valueOf(it.asString)) }

                get(PARTS_FIELD).asJsonObject.entrySet()
                    .forEach { parts[ChatMessagePart.valueOf(it.key)] = it.value.asString }

                val permission = get(PERMISSION_FIELD)?.asString
                val request = ChatMessageRequest(targetPlayers, permission, targetServers, parts)

                request
            }
        }
    }
}