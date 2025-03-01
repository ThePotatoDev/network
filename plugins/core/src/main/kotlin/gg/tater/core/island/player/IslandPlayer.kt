package gg.tater.core.island.player

import com.google.gson.*
import gg.tater.core.island.player.position.PositionDirector
import gg.tater.core.position.WrappedPosition
import gg.tater.core.server.model.ServerType
import java.lang.reflect.Type
import java.util.*

open class IslandPlayer(
    val uuid: UUID,
    val name: String,
    open var islandId: UUID? = null,
    private val spawns: MutableMap<ServerType, WrappedPosition> = mutableMapOf()
) {

    private companion object {
        const val UUID_FIELD = "uuid"
        const val NAME_FIELD = "name"
        const val ISLAND_ID_FIELD = "island_id"
        const val SPAWNS_FIELD = "spawns"
    }

    fun setNextServerSpawnPos(type: ServerType, director: PositionDirector, position: WrappedPosition): IslandPlayer {
        spawns[type] = position
        return this
    }

    class Adapter : JsonSerializer<IslandPlayer>, JsonDeserializer<IslandPlayer> {
        override fun serialize(player: IslandPlayer, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(UUID_FIELD, player.uuid.toString())
                addProperty(NAME_FIELD, player.name)

                if (player.islandId != null) {
                    addProperty(ISLAND_ID_FIELD, player.islandId.toString())
                }
            }
        }

        override fun deserialize(element: JsonElement, type: Type, context: JsonDeserializationContext): IslandPlayer {
            return (element as JsonObject).let {
                val uuid = UUID.fromString(it.get(UUID_FIELD).asString)
                val name = it.get(NAME_FIELD).asString
                var islandId: UUID? = null

                if (it.has(ISLAND_ID_FIELD)) {
                    islandId = UUID.fromString(it.get(ISLAND_ID_FIELD).asString)
                }

                IslandPlayer(uuid, name, islandId)
            }
        }
    }
}
