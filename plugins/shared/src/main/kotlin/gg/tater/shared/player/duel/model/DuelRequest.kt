package gg.tater.shared.player.duel.model

import com.google.gson.*
import java.lang.reflect.Type
import java.util.*

data class DuelRequest(
    val teamOne: MutableSet<UUID>,
    val teamTwo: MutableSet<UUID>,
    val kit: DuelKit,
    val map: DuelMap
) {

    private companion object {
        const val TEAM_ONE_FIELD = "team_one"
        const val TEAM_TWO_FIELD = "team_two"
        const val KIT_FIELD = "kit"
    }

    class Adapter : JsonSerializer<DuelRequest>, JsonDeserializer<DuelRequest> {
        override fun serialize(request: DuelRequest, type: Type, context: JsonSerializationContext): JsonElement {
            TODO("Not yet implemented")
        }

        override fun deserialize(element: JsonElement, type: Type, context: JsonDeserializationContext): DuelRequest {
            TODO("Not yet implemented")
        }
    }
}
