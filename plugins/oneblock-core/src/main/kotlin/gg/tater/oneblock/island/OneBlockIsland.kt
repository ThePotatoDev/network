package gg.tater.oneblock.island

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.island.Island
import java.lang.reflect.Type
import java.util.*

class OneBlockIsland(id: UUID, ownerId: UUID, ownerName: String) : Island(id, ownerId, ownerName) {

    private companion object {

    }

    @JsonAdapter(OneBlockIsland::class)
    class Adapter : JsonSerializer<OneBlockIsland>, JsonDeserializer<OneBlockIsland> {
        override fun serialize(island: OneBlockIsland, type: Type, context: JsonSerializationContext): JsonElement {
            TODO("Not yet implemented")
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): OneBlockIsland {
            TODO("Not yet implemented")
        }
    }
}