package gg.tater.core.island.experience.player

import com.google.gson.*
import gg.tater.core.JsonAdapter
import gg.tater.core.Mapping
import java.lang.reflect.Type
import java.util.*

@Mapping("experience_players")
data class ExperiencePlayer(
    val uuid: UUID,
    var meta: MutableMap<String, String> = mutableMapOf()
) {

    companion object {
        private const val UUID_FIELD = "uuid"
        private const val META_FIELD = "meta"
        const val STAGE_PROGRESS = "stage_progress"
    }

    fun getMetaValue(key: String): String? {
        return meta[key]
    }

    fun setMeta(key: String, value: Any) {
        meta[key] = value.toString()
    }

    fun hasMetaEqualTo(key: String, value: Any): Boolean {
        return meta[key] == value.toString()
    }

    @JsonAdapter(ExperiencePlayer::class)
    class Adapter : JsonSerializer<ExperiencePlayer>, JsonDeserializer<ExperiencePlayer> {
        override fun serialize(player: ExperiencePlayer, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                val meta = JsonArray()

                addProperty(UUID_FIELD, player.uuid.toString())

                for (entry in player.meta) {
                    meta.add(JsonObject().apply {
                        addProperty(entry.key, entry.value)
                    })
                }

                add(META_FIELD, meta)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): ExperiencePlayer {
            return (element as JsonObject).let {
                val uuid = UUID.fromString(it.get(UUID_FIELD).asString)
                val meta = mutableMapOf<String, String>()

                it.get(META_FIELD).asJsonArray.forEach { ele ->
                    for (entry in ele.asJsonObject.entrySet()) {
                        meta[entry.key] = entry.value.asString
                    }
                }

                ExperiencePlayer(uuid, meta)
            }
        }
    }
}