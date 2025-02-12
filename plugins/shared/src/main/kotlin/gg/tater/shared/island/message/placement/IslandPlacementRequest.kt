package gg.tater.shared.island.message.placement

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.island.Island
import gg.tater.shared.network.model.server.ServerDataModel
import gg.tater.shared.redis.Redis
import org.bukkit.entity.Player
import java.lang.reflect.Type
import java.util.*

@Redis.Mapping("island_placement_req")
@Redis.ReqRes("island_actions")
class IslandPlacementRequest(
    val server: String,
    val playerId: UUID,
    val islandId: UUID,
    val name: String,
    var internal: Boolean = false
) {

    companion object {
        const val SERVER_FIELD = "server"
        const val PLAYER_ID_FIELD = "player_id"
        const val NAME_FIELD = "name"
        const val INTERNAL_FIELD = "login"
        const val ISLAND_ID_FIELD = "island_id"

        fun of(sender: Player, island: Island, server: ServerDataModel): IslandPlacementRequest {
            return IslandPlacementRequest(server.id, sender.uniqueId, island.id, sender.name, true)
        }
    }

    @JsonAdapter(IslandPlacementRequest::class)
    class Adapter : JsonSerializer<IslandPlacementRequest>, JsonDeserializer<IslandPlacementRequest> {
        override fun serialize(
            model: IslandPlacementRequest,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {
                addProperty(SERVER_FIELD, model.server)
                addProperty(PLAYER_ID_FIELD, model.playerId.toString())
                addProperty(NAME_FIELD, model.name)
                addProperty(INTERNAL_FIELD, model.internal)
                addProperty(ISLAND_ID_FIELD, model.islandId.toString())
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): IslandPlacementRequest {
            return (element as JsonObject).run {
                val server = get(SERVER_FIELD).asString
                val playerId = UUID.fromString(get(PLAYER_ID_FIELD).asString)
                val name = get(NAME_FIELD).asString
                val internal = get(INTERNAL_FIELD).asBoolean
                val islandId = UUID.fromString(get(ISLAND_ID_FIELD).asString)
                IslandPlacementRequest(server, playerId, islandId, name, internal)
            }
        }
    }
}