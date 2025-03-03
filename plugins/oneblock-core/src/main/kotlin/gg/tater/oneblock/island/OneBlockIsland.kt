package gg.tater.oneblock.island

import com.google.gson.*
import gg.tater.core.JsonAdapter
import gg.tater.core.Mapping
import gg.tater.core.island.Island
import gg.tater.core.position.WrappedPosition
import java.lang.reflect.Type
import java.util.*

@Mapping("oneblock_islands")
class OneBlockIsland(id: UUID, ownerId: UUID, ownerName: String, var level: Int = 1, var ftue: Boolean = true) : Island(
    id,
    ownerId,
    ownerName,
    spawn = WrappedPosition(0.0, 70.0, 0.0, 0F, 0F)
) {

    companion object {
        // Breakable block location is a block below the default spawn for OneBlock islands
        val ONE_BLOCK_LOCATION = WrappedPosition(0.0, 69.0, 0.0, 0F, 0F)

        private const val LEVEL_FIELD = "island_level"
        private const val FTUE_FIELD = "ftue"
    }

    @JsonAdapter(OneBlockIsland::class)
    class Adapter : JsonSerializer<OneBlockIsland>, JsonDeserializer<OneBlockIsland> {
        private val baseAdapter = Island.Adapter()

        override fun serialize(island: OneBlockIsland, type: Type, context: JsonSerializationContext): JsonElement {
            return (baseAdapter.serialize(island, type, context) as JsonObject).apply {
                addProperty(LEVEL_FIELD, island.level)
                addProperty(FTUE_FIELD, island.ftue)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): OneBlockIsland {
            val island = baseAdapter.deserialize(element, type, context)
            return (element as JsonObject).let {
                val level = it.get(LEVEL_FIELD).asInt
                val ftue = it.get(FTUE_FIELD).asBoolean
                OneBlockIsland(island.id, island.ownerId, island.ownerName, level, ftue)
            }
        }
    }
}