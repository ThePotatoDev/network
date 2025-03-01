package gg.tater.core.island.player

import com.google.gson.*
import gg.tater.core.Json
import gg.tater.core.island.player.position.PositionDirector
import gg.tater.core.island.player.position.SpawnPositionData
import gg.tater.core.position.WrappedPosition
import gg.tater.core.server.model.ServerType
import java.lang.reflect.Type
import java.util.*

open class IslandPlayer(
    val uuid: UUID,
    val name: String,
    open var lastServerType: ServerType,
    open var islandId: UUID? = null,
    val spawns: MutableMap<ServerType, SpawnPositionData> = mutableMapOf()
) {

    private companion object {
        const val UUID_FIELD = "uuid"
        const val NAME_FIELD = "name"
        const val ISLAND_ID_FIELD = "island_id"
        const val SPAWNS_FIELD = "spawns"
        const val LAST_SERVER_TYPE_FIELD = "last_server_type"
    }

    fun setServerSpawnPos(
        type: ServerType,
        director: PositionDirector,
        position: WrappedPosition,
        meta: MutableMap<String, String>
    ): IslandPlayer {
        spawns[type] = SpawnPositionData(director, position, meta)
        return this
    }

    fun setServerSpawnPos(
        type: ServerType,
        director: PositionDirector,
        position: WrappedPosition
    ): IslandPlayer {
        spawns[type] = SpawnPositionData(director, position)
        return this
    }

    fun getServerSpawnPos(type: ServerType): SpawnPositionData? {
        return spawns[type]
    }

    fun consumeSpawnPos(type: ServerType): SpawnPositionData? {
        return spawns.remove(type)
    }

    class Adapter : JsonSerializer<IslandPlayer>, JsonDeserializer<IslandPlayer> {
        override fun serialize(player: IslandPlayer, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                val spawns = JsonArray()

                for (entry in player.spawns) {
                    spawns.add(JsonObject().apply {
                        addProperty(entry.key.name, Json.get().toJson(entry.value))
                    })
                }

                addProperty(UUID_FIELD, player.uuid.toString())
                addProperty(NAME_FIELD, player.name)
                addProperty(LAST_SERVER_TYPE_FIELD, player.lastServerType.name)
                add(SPAWNS_FIELD, spawns)

                if (player.islandId != null) {
                    addProperty(ISLAND_ID_FIELD, player.islandId.toString())
                }
            }
        }

        override fun deserialize(element: JsonElement, type: Type, context: JsonDeserializationContext): IslandPlayer {
            return (element as JsonObject).let {
                val uuid = UUID.fromString(it.get(UUID_FIELD).asString)
                val name = it.get(NAME_FIELD).asString
                val lastServerType = ServerType.valueOf(it.get(LAST_SERVER_TYPE_FIELD).asString)
                var islandId: UUID? = null

                if (it.has(ISLAND_ID_FIELD)) {
                    islandId = UUID.fromString(it.get(ISLAND_ID_FIELD).asString)
                }

                val player = IslandPlayer(uuid, name, lastServerType, islandId, mutableMapOf())

                if (it.has(SPAWNS_FIELD)) {
                    for (spawnData in it.get(SPAWNS_FIELD).asJsonArray.map { ele -> ele.asJsonObject }) {
                        for (entry in spawnData.asMap()) {
                            val serverType = ServerType.valueOf(entry.key)
                            val data = Json.get().fromJson(entry.value.asString, SpawnPositionData::class.java)
                            player.spawns[serverType] = data
                        }
                    }
                }

                player
            }
        }
    }
}
