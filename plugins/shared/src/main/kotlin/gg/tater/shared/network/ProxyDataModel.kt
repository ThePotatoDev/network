package gg.tater.shared.network

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import java.lang.reflect.Type

@Mapping("proxy_data_model")
data class ProxyDataModel(var players: Int = 0) {

    private companion object {
        const val PLAYERS_FIELD = "players"
    }

    @JsonAdapter(ProxyDataModel::class)
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