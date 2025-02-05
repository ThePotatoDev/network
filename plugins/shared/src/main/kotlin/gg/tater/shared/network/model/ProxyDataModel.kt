package gg.tater.shared.network.model

import com.google.gson.*
import java.lang.reflect.Type

data class ProxyDataModel(var players: Int = 0) {

    private companion object {
        const val PLAYERS_FIELD = "players"
    }

    class Adapter : JsonSerializer<ProxyDataModel>, JsonDeserializer<ProxyDataModel> {
        override fun serialize(data: ProxyDataModel, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(PLAYERS_FIELD, data.players)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): ProxyDataModel {
            return (element as JsonObject).let {
                val players = it.get(PLAYERS_FIELD).asInt
                ProxyDataModel(players)
            }
        }
    }
}