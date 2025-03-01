package gg.tater.core.island.player.position

import com.google.gson.*
import gg.tater.core.Json
import gg.tater.core.JsonAdapter
import gg.tater.core.position.WrappedPosition
import java.lang.reflect.Type

data class SpawnPositionData(
    val director: PositionDirector,
    val position: WrappedPosition,
    val meta: MutableMap<String, String> = mutableMapOf()
) {

    companion object {
        const val ISLAND_ID_META_KEY = "island-id"
        const val WORLD_NAME_META_KEY = "world-name"

        private const val DIRECTOR_FIELD = "director"
        private const val POSITION_FIELD = "position"
        private const val META_FIELD = "meta"
    }

    fun getMetaValue(key: String): String? {
        return meta[key]
    }

    @JsonAdapter(SpawnPositionData::class)
    class Adapter : JsonSerializer<SpawnPositionData>, JsonDeserializer<SpawnPositionData> {
        override fun serialize(data: SpawnPositionData, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                val meta = JsonArray()

                addProperty(DIRECTOR_FIELD, data.director.name)
                addProperty(POSITION_FIELD, Json.get().toJson(data.position))

                for (entry in data.meta.entries) {
                    meta.add(JsonObject().apply {
                        addProperty(entry.key, entry.value)
                    })
                }

                add(META_FIELD, meta)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): SpawnPositionData {
            return (element as JsonObject).let {
                val director = PositionDirector.valueOf(it.get(DIRECTOR_FIELD).asString)
                val position = Json.get().fromJson(it.get(POSITION_FIELD).asString, WrappedPosition::class.java)
                val data = SpawnPositionData(director, position)

                for (obj in it.get(META_FIELD).asJsonArray.map { e -> e.asJsonObject }) {
                    for (entry in obj.asMap()) {
                        data.meta[entry.key] = entry.value.asString
                    }
                }

                data
            }
        }

    }

}
