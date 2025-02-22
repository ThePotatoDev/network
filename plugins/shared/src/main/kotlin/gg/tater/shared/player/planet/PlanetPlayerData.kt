package gg.tater.shared.player.planet

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import java.lang.reflect.Type
import java.util.*

@Mapping("planet_player_data")
data class PlanetPlayerData(val uuid: UUID) {

    private companion object {
        const val UUID_FIELD = "uuid"
    }

    @JsonAdapter(PlanetPlayerData::class)
    class Adapter : JsonSerializer<PlanetPlayerData>, JsonDeserializer<PlanetPlayerData> {
        override fun serialize(data: PlanetPlayerData, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(UUID_FIELD, data.uuid.toString())
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): PlanetPlayerData {
            return (element as JsonObject).let {
                val uuid = UUID.fromString(it.get(UUID_FIELD).asString)
                PlanetPlayerData(uuid)
            }
        }
    }
}