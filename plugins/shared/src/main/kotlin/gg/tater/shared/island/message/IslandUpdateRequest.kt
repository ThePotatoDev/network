package gg.tater.shared.island.message

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.island.Island
import gg.tater.shared.redis.Redis
import java.lang.reflect.Type
import java.util.*

@Redis.Mapping("island_update_req")
@Redis.ReqRes("island_actions")
class IslandUpdateRequest(val islandId: UUID, var server: String?) {

    companion object {
        const val SERVER_FIELD = "server"
        const val ISLAND_ID_FIELD = "island_id"
    }

    constructor(island: Island) : this(island.id, island.currentServerId)

    @JsonAdapter(IslandUpdateRequest::class)
    class Adapter : JsonSerializer<IslandUpdateRequest>, JsonDeserializer<IslandUpdateRequest> {
        override fun serialize(
            model: IslandUpdateRequest,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {
                model.server?.let { addProperty(SERVER_FIELD, it) }
                addProperty(ISLAND_ID_FIELD, model.islandId.toString())
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): IslandUpdateRequest {
            return (element as JsonObject).run {
                val server = get(SERVER_FIELD)?.asString
                val islandId = UUID.fromString(get(ISLAND_ID_FIELD).asString)
                IslandUpdateRequest(islandId, server)
            }
        }
    }
}