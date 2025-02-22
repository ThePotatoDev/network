package gg.tater.shared.island.message

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import gg.tater.shared.annotation.Message
import gg.tater.shared.redis.Redis
import java.lang.reflect.Type
import java.util.*

@Mapping("island_delete_req")
@Message("island_actions")
class IslandDeleteRequest(
    val islandId: UUID,
    var server: String?,
) {

    companion object {
        const val SERVER_FIELD = "server"
        const val ISLAND_ID_FIELD = "island_id"
    }

    @JsonAdapter(IslandDeleteRequest::class)
    class Adapter : JsonSerializer<IslandDeleteRequest>, JsonDeserializer<IslandDeleteRequest> {
        override fun serialize(
            model: IslandDeleteRequest,
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
        ): IslandDeleteRequest {
            return (element as JsonObject).run {
                val server = get(SERVER_FIELD)?.asString
                val islandId = UUID.fromString(get(ISLAND_ID_FIELD).asString)
                IslandDeleteRequest(islandId, server)
            }
        }
    }
}