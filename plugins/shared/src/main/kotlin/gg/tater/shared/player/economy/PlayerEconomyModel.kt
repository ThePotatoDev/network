package gg.tater.shared.player.economy

import com.google.gson.*
import java.lang.reflect.Type
import java.util.*

data class PlayerEconomyModel(val uuid: UUID, val balance: MutableMap<EconomyType, Double> = mutableMapOf()) {

    companion object {
        const val UUID_FIELD = "uuid"
        const val BALANCE_FIELD = "balance"
    }

    fun get(type: EconomyType): Double {
        return balance.getOrDefault(type, 0.0)
    }

    fun withdraw(type: EconomyType, amount: Double) {
        balance.computeIfPresent(type) { _, current ->
            return@computeIfPresent current - amount
        }
    }

    fun add(type: EconomyType, amount: Double) {
        balance.computeIfPresent(type) { _, current ->
            return@computeIfPresent current + amount
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
            val json = element as JsonObject
            val uuid = UUID.fromString(json.get(UUID_FIELD).asString)
            val balance = mutableMapOf<EconomyType, Double>()
            json.getAsJsonObject(BALANCE_FIELD)
                .entrySet()
                .forEach { (key, value) ->
                    balance[EconomyType.valueOf(key)] = value.asDouble
                }
            return PlayerEconomyModel(uuid, balance)
        }
    }

}