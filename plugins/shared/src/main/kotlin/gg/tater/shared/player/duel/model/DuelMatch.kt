package gg.tater.shared.player.duel.model

import com.google.gson.*
import java.lang.reflect.Type
import java.util.*

enum class State {
    STARTING,
    IN_GAME,
    ENDED
}

data class DuelMatch(val id: UUID, val request: DuelRequest, var state: State = State.STARTING) {

    private companion object {
        const val ID_FIELD = "id"
        const val REQUEST_FIELD = "request"
        const val STATE_FIELD = "state"
    }

    fun prepare() {

    }

    fun start() {

    }

    fun end() {

    }

    class Adapter : JsonSerializer<DuelMatch>, JsonDeserializer<DuelMatch> {
        override fun serialize(match: DuelMatch, type: Type, context: JsonSerializationContext): JsonElement {
            TODO("Not yet implemented")
        }

        override fun deserialize(element: JsonElement, type: Type, context: JsonDeserializationContext): DuelMatch {
            TODO("Not yet implemented")
        }
    }
}