package gg.tater.core.island.experience.player

import com.google.gson.*
import gg.tater.core.JsonAdapter
import java.lang.reflect.Type
import java.util.*

data class ExperiencePlayer(
    val uuid: UUID,
    var currentStageId: Int,
    var progressMeta: MutableMap<String, String> = mutableMapOf()
) {

    private companion object {
        const val UUID_FIELD = "uuid"
        const val CURRENT_STAGE_ID_FIELD = "current_stage_id"
        const val PROGRESS_META_FIELD = "progress_meta"
    }

    fun getMetaValue(key: String): String? {
        return progressMeta[key]
    }

    fun setMeta(key: String, value: String) {
        progressMeta[key] = value
    }

    fun hasMetaEqualTo(key: String, value: Any): Boolean {
        return progressMeta[key] == value.toString()
    }

    @JsonAdapter(ExperiencePlayer::class)
    class Adapter : JsonSerializer<ExperiencePlayer>, JsonDeserializer<ExperiencePlayer> {
        override fun serialize(player: ExperiencePlayer, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(UUID_FIELD, player.uuid.toString())
                addProperty(CURRENT_STAGE_ID_FIELD, player.currentStageId)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): ExperiencePlayer {
            TODO("Not yet implemented")
        }
    }
}