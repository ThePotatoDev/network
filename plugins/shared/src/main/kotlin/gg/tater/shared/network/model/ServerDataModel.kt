package gg.tater.shared.network.model

import com.google.gson.*
import gg.tater.shared.redis.Redis
import java.lang.reflect.Type

@Redis.Mapping("server_data_model")
data class ServerDataModel(
    val id: String,
    val type: ServerType,
    var state: ServerState = ServerState.READY,
    var usedMemory: Long = 0,
    var players: Int = 0
) {

    companion object {
        const val MAX_SERVER_MEMORY = 1024.0
        const val SERVER_MEMORY_PER_WORLD = 50.0

        const val ID_FIELD = "id"
        const val TYPE_FIELD = "type"
        const val STATE_FIELD = "state"
        const val USED_MEMORY_FIELD = "used_memory"
        const val PLAYERS_FIELD = "players"
    }

    class Adapter : JsonSerializer<ServerDataModel>, JsonDeserializer<ServerDataModel> {
        override fun serialize(model: ServerDataModel, type: Type, context: JsonSerializationContext): JsonElement {
            val json = JsonObject()
            json.addProperty(ID_FIELD, model.id)
            json.addProperty(TYPE_FIELD, model.type.name)
            json.addProperty(STATE_FIELD, model.state.name)
            json.addProperty(USED_MEMORY_FIELD, model.usedMemory)
            json.addProperty(PLAYERS_FIELD, model.players)
            return json
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): ServerDataModel {
            val json = element as JsonObject
            val id = json.get(ID_FIELD).asString
            val serverType = ServerType.valueOf(json.get(TYPE_FIELD).asString)
            val state = ServerState.valueOf(json.get(STATE_FIELD).asString)
            val usedMemory = json.get(USED_MEMORY_FIELD).asLong
            val players = json.get(PLAYERS_FIELD).asInt
            return ServerDataModel(id, serverType, state, usedMemory, players)
        }
    }
}