package gg.tater.shared.player.progression

import com.google.gson.*
import gg.tater.shared.redis.Redis
import gg.tater.shared.player.progression.skill.model.SkillType
import java.lang.reflect.Type
import java.util.*

@Redis.Mapping("player_progression_data")
data class PlayerProgressDataModel(
    val uuid: UUID,
    val progress: MutableMap<SkillType, Double> = mutableMapOf(),
    val levels: MutableMap<SkillType, Int> = mutableMapOf()
) {
    companion object {
        const val UUID_FIELD = "uuid"
        const val PROGRESS_FIELD = "progress"
        const val LEVELS_FIELD = "levels"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PlayerProgressDataModel) return false
        return other.uuid == this.uuid
    }

    fun getProgress(type: SkillType): Double {
        return progress[type] ?: 0.0
    }

    fun setProgress(type: SkillType, value: Double) {
        progress[type] = value
    }

    fun addProgress(type: SkillType, value: Double) {
        progress[type] = (progress[type] ?: 0.0) + value
    }

    fun getLevel(type: SkillType): Int {
        return levels[type] ?: 1
    }

    fun setLevel(type: SkillType, value: Int) {
        levels[type] = value
    }

    class Adapter : JsonSerializer<PlayerProgressDataModel>, JsonDeserializer<PlayerProgressDataModel> {
        override fun serialize(
            model: PlayerProgressDataModel,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val json = JsonObject()
            val progress = JsonObject()
            val levels = JsonObject()
            json.addProperty(UUID_FIELD, model.uuid.toString())

            model.progress.forEach { (type, value) ->
                progress.addProperty(type.name, value)
            }

            model.levels.forEach { (type, value) ->
                levels.addProperty(type.name, value)
            }

            json.add(LEVELS_FIELD, levels)
            json.add(PROGRESS_FIELD, progress)
            return json
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): PlayerProgressDataModel {
            val json = element as JsonObject
            val uuid = UUID.fromString(json.get(UUID_FIELD).asString)

            val progress = mutableMapOf<SkillType, Double>()
            val levels = mutableMapOf<SkillType, Int>()

            json.getAsJsonObject(PROGRESS_FIELD).entrySet().forEach { (key, value) ->
                progress[SkillType.valueOf(key)] = value.asDouble
            }

            json.getAsJsonObject(LEVELS_FIELD).entrySet().forEach { (key, value) ->
                levels[SkillType.valueOf(key)] = value.asInt
            }

            return PlayerProgressDataModel(uuid, progress, levels)
        }
    }
}
