package gg.tater.shared.player.teleport

import com.google.gson.*
import gg.tater.shared.Json
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import gg.tater.shared.position.WrappedPosition
import java.lang.reflect.Type
import java.util.*

@Mapping("teleport_request")
data class TeleportRequest(
    val id: UUID,
    val senderId: UUID,
    val targetId: UUID,
    val position: WrappedPosition,
    val server: String,
    val direction: TeleportDirection,
    var state: TeleportState = TeleportState.PENDING
) {

    private companion object {
        const val ID_FIELD = "id"
        const val SENDER_ID_FIELD = "sender_id"
        const val TARGET_ID_FIELD = "target_id"
        const val POSITION_FIELD = "position"
        const val SERVER_FIELD = "server"
        const val DIRECTION_FIELD = "direction"
        const val STATE_FIELD = "state"
    }

    enum class TeleportDirection {
        TELEPORT,
        TELEPORT_HERE
    }

    enum class TeleportState {
        PENDING,
        ACCEPTED,
        DENIED
    }

    @JsonAdapter(TeleportRequest::class)
    class Adapter : JsonSerializer<TeleportRequest>, JsonDeserializer<TeleportRequest> {
        override fun serialize(request: TeleportRequest, type: Type, context: JsonSerializationContext?): JsonElement {
            return JsonObject().apply {
                addProperty(ID_FIELD, request.id.toString())
                addProperty(SENDER_ID_FIELD, request.senderId.toString())
                addProperty(TARGET_ID_FIELD, request.targetId.toString())
                addProperty(POSITION_FIELD, Json.INSTANCE.toJson(request.position))
                addProperty(SERVER_FIELD, request.server)
                addProperty(STATE_FIELD, request.state.name)
                addProperty(DIRECTION_FIELD, request.direction.name)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): TeleportRequest {
            (element as JsonObject).let {
                val id = UUID.fromString(it.get(ID_FIELD).asString)
                val senderId = UUID.fromString(it.get(SENDER_ID_FIELD).asString)
                val targetId = UUID.fromString(it.get(TARGET_ID_FIELD).asString)
                val position = Json.INSTANCE.fromJson(it.get(POSITION_FIELD).asString, WrappedPosition::class.java)
                val server = it.get(SERVER_FIELD).asString
                val state = TeleportState.valueOf(it.get(STATE_FIELD).asString)
                val direction = TeleportDirection.valueOf(it.get(DIRECTION_FIELD).asString)
                return TeleportRequest(id, senderId, targetId, position, server, direction, state)
            }
        }
    }
}
