package gg.tater.oneblock.island

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.island.Island
import gg.tater.shared.position.WrappedPosition
import java.lang.reflect.Type
import java.util.*

class OneBlockIsland(id: UUID, ownerId: UUID, ownerName: String, var level: Int = 1) : Island(
    id,
    ownerId,
    ownerName,
    spawn = WrappedPosition(0.0, 80.0, 0.0, 0F, 0F)
) {

    private companion object {
        const val LEVEL_FIELD = "island_level"
    }

    @JsonAdapter(OneBlockIsland::class)
    class Adapter : JsonSerializer<OneBlockIsland>, JsonDeserializer<OneBlockIsland> {
        private val baseAdapter = Island.Adapter()

        override fun serialize(island: OneBlockIsland, type: Type, context: JsonSerializationContext): JsonElement {
            return (baseAdapter.serialize(island, type, context) as JsonObject).apply {
                addProperty(LEVEL_FIELD, island.level)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): OneBlockIsland {
            val island = baseAdapter.deserialize(element, type, context)
            val level = (element as JsonObject).get(LEVEL_FIELD).asInt
            return OneBlockIsland(island.id, island.ownerId, island.ownerName, level)
        }
    }
}