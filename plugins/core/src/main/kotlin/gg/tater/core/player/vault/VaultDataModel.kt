package gg.tater.core.player.vault

import com.google.gson.*
import gg.tater.core.JsonAdapter
import gg.tater.core.annotation.Mapping
import me.lucko.helper.serialize.Serializers
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type
import java.util.*

@Mapping("vault_data_model")
data class VaultDataModel(
    val uuid: UUID,
    var amount: Int = DEFAULT_VAULT_AMOUNT,
    val vaults: MutableMap<Int, Array<out ItemStack?>> = mutableMapOf()
) {

    companion object {
        const val DEFAULT_VAULT_AMOUNT = 1
        private const val UUID_FIELD = "uuid"
        private const val AMOUNT_FIELD = "amount"
        private const val VAULTS_FIELD = "vaults"
    }

    fun getVaultItems(id: Int): Array<out ItemStack?> {
        return vaults.getOrDefault(id, emptyArray())
    }

    fun setVaultItems(id: Int, items: Array<out ItemStack?>) {
        vaults[id] = items
    }

    fun addVaults(count: Int) {
        amount += count
    }

    fun setVaults(count: Int) {
        amount = count
    }

    @JsonAdapter(VaultDataModel::class)
    class Adapter : JsonSerializer<VaultDataModel>, JsonDeserializer<VaultDataModel> {
        override fun serialize(model: VaultDataModel, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                addProperty(UUID_FIELD, model.uuid.toString())
                addProperty(AMOUNT_FIELD, model.amount)

                if (model.vaults.isNotEmpty()) {
                    add(VAULTS_FIELD, JsonArray().apply {
                        for (entry in model.vaults) {
                            add(JsonObject().apply {
                                addProperty("id", entry.key)
                                add("stacks", Serializers.serializeItemstacks(entry.value))
                            })
                        }
                    })
                }
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): VaultDataModel {
            (element as JsonObject).apply {
                val uuid = UUID.fromString(get(UUID_FIELD).asString)
                val amount = get(AMOUNT_FIELD).asInt
                val data = VaultDataModel(uuid, amount)

                if (has(VAULTS_FIELD)) {
                    get(VAULTS_FIELD).asJsonArray.forEach {
                        val obj = it.asJsonObject
                        val id = obj.get("id").asInt
                        val stacks = Serializers.deserializeItemstacks(obj.get("stacks").asJsonPrimitive)
                        data.setVaultItems(id, stacks)
                    }
                }

                return data
            }
        }
    }
}