package gg.tater.shared.player.duel.model

import com.google.gson.*
import gg.tater.shared.player.position.WrappedPosition
import java.lang.reflect.Type

enum class SpawnArea {
    TEAM_ONE,
    TEAM_TWO
}

data class DuelMap(val id: String, val worldName: String, val spawns: MutableMap<SpawnArea, WrappedPosition>) {

    private companion object {
        const val ID_FIELD = "id"
        const val WORLD_NAME_FIELD = "world_name"
        const val SPAWNS_FIELD = "spawns"
    }

    class Adapter : JsonSerializer<DuelMap>, JsonDeserializer<DuelMap> {

        override fun serialize(map: DuelMap, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(ID_FIELD, map.id)
                addProperty(WORLD_NAME_FIELD, map.worldName)
            }
        }

        override fun deserialize(element: JsonElement, type: Type, context: JsonDeserializationContext): DuelMap {
            TODO("Not yet implemented")
        }
    }
}