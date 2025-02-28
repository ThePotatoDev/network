package gg.tater.oneblock.player

import com.google.gson.*
import gg.tater.core.JsonAdapter
import gg.tater.core.Mapping
import gg.tater.core.island.player.IslandPlayer
import java.lang.reflect.Type
import java.util.*

@Mapping("oneblock_players")
class OneBlockPlayer(uuid: UUID, name: String, override var islandId: UUID? = null) :
    IslandPlayer(uuid, name, islandId) {

    private companion object {
        const val UUID_FIELD = "uuid"
        const val NAME_FIELD = "name"
        const val ISLAND_ID_FIELD = "island_id"
    }

    @JsonAdapter(OneBlockPlayer::class)
    class Adapter : JsonSerializer<OneBlockPlayer>, JsonDeserializer<OneBlockPlayer> {
//        private val baseAdapter = IslandPlayer.Adapter()

        override fun serialize(player: OneBlockPlayer, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(UUID_FIELD, player.uuid.toString())
                addProperty(NAME_FIELD, player.name)

                if (player.islandId != null) {
                    addProperty(ISLAND_ID_FIELD, player.islandId.toString())
                }
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): OneBlockPlayer {
            return (element as JsonObject).let {
                val uuid = UUID.fromString(it.get(UUID_FIELD).asString)
                val name = it.get(NAME_FIELD).asString
                var islandId: UUID? = null

                if (it.has(ISLAND_ID_FIELD)) {
                    islandId = UUID.fromString(it.get(ISLAND_ID_FIELD).asString)
                }

                OneBlockPlayer(uuid, name, islandId)
            }
        }
    }
}