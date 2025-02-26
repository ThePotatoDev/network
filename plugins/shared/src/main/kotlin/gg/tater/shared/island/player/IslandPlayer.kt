package gg.tater.shared.island.player

import com.google.gson.*
import java.lang.reflect.Type
import java.util.*

open class IslandPlayer(val uuid: UUID, val name: String, val islandId: UUID?) {

    private companion object {
        const val UUID_FIELD = "uuid"
        const val NAME_FIELD = "name"
        const val ISLAND_ID_FIELD = "island_id"
    }

    class Adapter : JsonSerializer<IslandPlayer>, JsonDeserializer<IslandPlayer> {
        override fun serialize(player: IslandPlayer, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(UUID_FIELD, player.uuid.toString())
                addProperty(NAME_FIELD, player.name)
                addProperty(ISLAND_ID_FIELD, player.islandId.toString())
            }
        }

        override fun deserialize(element: JsonElement, type: Type, context: JsonDeserializationContext): IslandPlayer {
            return (element as JsonObject).let {
                val uuid = UUID.fromString(it.get(UUID_FIELD).asString)
                val name = it.get(NAME_FIELD).asString
                val islandId = UUID.fromString(it.get(ISLAND_ID_FIELD).asString)
                IslandPlayer(uuid, name, islandId)
            }
        }
    }
}
