package gg.tater.oneblock.player

import com.google.gson.*
import gg.tater.core.JsonAdapter
import gg.tater.core.island.player.IslandPlayer
import java.lang.reflect.Type
import java.util.*

class OneBlockPlayer(uuid: UUID, name: String, islandId: UUID?) : IslandPlayer(uuid, name, islandId) {

    private companion object {
        const val UUID_FIELD = "uuid"
        const val NAME_FIELD = "name"
        const val ISLAND_ID_FIELD = "island_id"
    }

    @JsonAdapter(OneBlockPlayer::class)
    class Adapter : JsonSerializer<OneBlockPlayer>, JsonDeserializer<OneBlockPlayer> {
        private val baseAdapter = IslandPlayer.Adapter()

        override fun serialize(player: OneBlockPlayer, type: Type, context: JsonSerializationContext): JsonElement {
            return baseAdapter.serialize(player, type, context)
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): OneBlockPlayer {
            TODO("Not yet implemented")
        }
    }
}