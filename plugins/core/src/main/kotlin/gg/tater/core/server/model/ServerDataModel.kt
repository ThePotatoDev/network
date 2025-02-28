package gg.tater.core.server.model

import com.google.gson.*
import gg.tater.core.JsonAdapter
import gg.tater.core.annotation.Mapping
import java.lang.reflect.Type

@Mapping("server_data_model")
data class ServerDataModel(
    val id: String,
    val type: ServerType,
    var state: ServerState = ServerState.READY,
    var freeMemory: Long = 0,
    var maxMemory: Long = 0,
    var players: Int = 0
) {

    companion object {
        const val MAX_MEMORY_THRESHOLD_PERCENTAGE = 80.0

        const val ID_FIELD = "id"
        const val TYPE_FIELD = "type"
        const val STATE_FIELD = "state"
        const val FREE_MEMORY_FIELD = "free_memory"
        const val MAX_MEMORY_FIELD = "max_memory"
        const val PLAYERS_FIELD = "players"
    }

    fun getUsedMemory(): Long {
        return maxMemory - freeMemory
    }

    @JsonAdapter(ServerDataModel::class)
    class Adapter : JsonSerializer<ServerDataModel>, JsonDeserializer<ServerDataModel> {
        override fun serialize(model: ServerDataModel, type: Type, context: JsonSerializationContext): JsonElement {
            val json = JsonObject()
            json.addProperty(ID_FIELD, model.id)
            json.addProperty(TYPE_FIELD, model.type.name)
            json.addProperty(STATE_FIELD, model.state.name)
            json.addProperty(FREE_MEMORY_FIELD, model.freeMemory)
            json.addProperty(MAX_MEMORY_FIELD, model.maxMemory)
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
            val freeMemory = json.get(FREE_MEMORY_FIELD).asLong
            val maxMemory = json.get(MAX_MEMORY_FIELD).asLong
            val players = json.get(PLAYERS_FIELD).asInt
            return ServerDataModel(id, serverType, state, freeMemory, maxMemory, players)
        }
    }
}