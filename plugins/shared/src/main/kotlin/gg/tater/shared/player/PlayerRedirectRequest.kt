package gg.tater.shared.player

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import gg.tater.shared.annotation.Message
import gg.tater.shared.redis.Redis
import gg.tater.shared.network.server.ServerType
import java.lang.reflect.Type
import java.util.*

@Mapping("player_redirect_req")
@Message("player_redirects")
data class PlayerRedirectRequest(val uuid: UUID, val type: ServerType, var server: String? = null) {

    companion object {
        const val UUID_FIELD = "uuid"
        const val TYPE_FIELD = "type"
        const val SERVER_FIELD = "server"
    }

    @JsonAdapter(PlayerRedirectRequest::class)
    class Adapter : JsonSerializer<PlayerRedirectRequest>, JsonDeserializer<PlayerRedirectRequest> {
        override fun serialize(
            model: PlayerRedirectRequest,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val json = JsonObject()
            json.addProperty(UUID_FIELD, model.uuid.toString())
            json.addProperty(TYPE_FIELD, model.type.name)
            model.server?.let { json.addProperty(SERVER_FIELD, it) }
            return json
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): PlayerRedirectRequest {
            val json = element as JsonObject
            val uuid = UUID.fromString(json.get(UUID_FIELD).asString)
            val serverType = ServerType.valueOf(json.get(TYPE_FIELD).asString)
            val server = json.get(SERVER_FIELD)?.asString
            return PlayerRedirectRequest(uuid, serverType, server)
        }
    }
}