package gg.tater.core.player.economy.model

import com.google.gson.*
import gg.tater.core.JsonAdapter
import gg.tater.core.Mapping
import java.lang.reflect.Type
import java.util.*

@Mapping("player_economy_model")
data class PlayerEconomyModel(
    val uuid: UUID, val balance: MutableMap<EconomyType, Double> = mutableMapOf(
        EconomyType.MONEY to 0.0
    )
) {

    companion object {
        const val UUID_FIELD = "uuid"
        const val BALANCE_FIELD = "balance"
    }

    fun get(type: EconomyType): Double {
        return balance.getOrDefault(type, 0.0)
    }

    fun withdraw(type: EconomyType, amount: Double): Double? {
        return balance.computeIfPresent(type) { _, current ->
            current - amount
        }
    }

    fun add(type: EconomyType, amount: Double): Double? {
        return balance.computeIfPresent(type) { _, current ->
            current + amount
        }
    }

    fun sub(type: EconomyType, amount: Double) {
        balance.computeIfPresent(type) { _, current ->
            var new = current - amount
            if (new < 0) {
                new = 0.0
            }
            return@computeIfPresent new
        }
    }

    fun set(type: EconomyType, amount: Double) {
        balance[type] = amount
    }

    @JsonAdapter(PlayerEconomyModel::class)
    class Adapter : JsonSerializer<PlayerEconomyModel>, JsonDeserializer<PlayerEconomyModel> {
        override fun serialize(
            model: PlayerEconomyModel,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val json = JsonObject()
            val balance = JsonObject()

            json.addProperty(UUID_FIELD, model.uuid.toString())
            model.balance.forEach { (type, amount) ->
                balance.addProperty(type.name, amount)
            }
            json.add(BALANCE_FIELD, balance)
            return json
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): PlayerEconomyModel {
            return (element as JsonObject).let {
                val uuid = UUID.fromString(it.get(UUID_FIELD).asString)
                val balance = mutableMapOf<EconomyType, Double>()

                it.getAsJsonObject(BALANCE_FIELD)
                    .entrySet()
                    .forEach { (key, value) ->
                        balance[EconomyType.valueOf(key)] = value.asDouble
                    }
                PlayerEconomyModel(uuid, balance)
            }
        }
    }

}