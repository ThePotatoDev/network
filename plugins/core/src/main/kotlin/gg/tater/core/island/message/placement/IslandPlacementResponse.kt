package gg.tater.core.island.message.placement

import com.google.gson.*
import gg.tater.core.Json
import gg.tater.core.JsonAdapter
import gg.tater.core.Mapping
import gg.tater.core.annotation.Message
import gg.tater.core.island.player.IslandPlayer
import java.lang.reflect.Type
import java.util.*

@Mapping("island_placement_resp")
@Message("island_actions")
class IslandPlacementResponse(val server: String, val playerId: UUID, val name: String, var internal: Boolean = false) {

    companion object {
        const val SERVER_FIELD = "server"
        const val PLAYER_ID_FIELD = "player_id"
        const val NAME_FIELD = "name"
        const val INTERNAL_FIELD = "internal"
    }

    @JsonAdapter(IslandPlacementResponse::class)
    class Adapter : JsonSerializer<IslandPlacementResponse>, JsonDeserializer<IslandPlacementResponse> {
        override fun serialize(
            model: IslandPlacementResponse,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val json = JsonObject()
            json.addProperty(SERVER_FIELD, model.server)
            json.addProperty(PLAYER_ID_FIELD, model.playerId.toString())
            json.addProperty(NAME_FIELD, model.name)
            json.addProperty(INTERNAL_FIELD, model.internal)
            return json
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): IslandPlacementResponse {
            val json = element as JsonObject
            val server = json.get(SERVER_FIELD).asString
            val playerId = UUID.fromString(json.get(PLAYER_ID_FIELD).asString)
            val name = json.get(NAME_FIELD).asString
            val internal = json.get(INTERNAL_FIELD).asBoolean
            return IslandPlacementResponse(server, playerId, name, internal)
        }
    }
}