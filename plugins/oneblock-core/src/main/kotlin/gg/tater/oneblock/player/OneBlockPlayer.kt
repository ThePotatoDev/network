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
            val base = baseAdapter.deserialize(element, type, context)
            val player = OneBlockPlayer(base.uuid, base.name, base.islandId)
            player.spawns.putAll(base.spawns)
            return player
        }
    }
}