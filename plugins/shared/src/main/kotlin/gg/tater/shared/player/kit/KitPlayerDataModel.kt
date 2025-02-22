package gg.tater.shared.player.kit

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import java.lang.reflect.Type
import java.time.Instant

@Mapping("kit_player_data_model")
class KitPlayerDataModel(private val lastUsed: MutableMap<String, Long> = mutableMapOf()) {

    companion object {
        const val LAST_USED_FIELD = "last_used"
    }

    @JsonAdapter(KitPlayerDataModel::class)
    class Adapter : JsonSerializer<KitPlayerDataModel>, JsonDeserializer<KitPlayerDataModel> {
        override fun serialize(data: KitPlayerDataModel, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                val lastUsed = JsonObject()

                for (entry in data.lastUsed.entries) {
                    lastUsed.addProperty(entry.key, entry.value)
                }

                add(LAST_USED_FIELD, lastUsed)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): KitPlayerDataModel {
            (element as JsonObject).let {
                val lastUsed: MutableMap<String, Long> = mutableMapOf()

                for (entry in it.get(LAST_USED_FIELD).asJsonObject.entrySet()) {
                    lastUsed[entry.key] = entry.value.asLong
                }

                return KitPlayerDataModel(lastUsed)
            }
        }
    }

    fun canUse(kit: KitDataModel): Boolean {
        val lastUsed = lastUsed[kit.name] ?: return true
        return Instant.now().isAfter(Instant.ofEpochMilli(lastUsed.plus(kit.cooldown)))
    }

    fun getLastUse(kit: KitDataModel): Long? {
        return lastUsed[kit.name]
    }

    fun setLastUsed(kit: KitDataModel): KitPlayerDataModel {
        lastUsed[kit.name] = Instant.now().toEpochMilli()
        return this
    }
}