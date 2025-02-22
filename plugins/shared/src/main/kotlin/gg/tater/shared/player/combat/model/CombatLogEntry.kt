package gg.tater.shared.player.combat.model

import com.google.gson.*
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.lang.reflect.Type
import java.util.*

@Mapping("combat_log_entry")
data class CombatLogEntry(val uuid: UUID, val inventory: Inventory) {

    private companion object {
        const val UUID_FIELD = "uuid"
        const val INVENTORY_FIELD = "inventory"
    }

    constructor(player: Player) : this(player.uniqueId, player.inventory)

    @JsonAdapter(CombatLogEntry::class)
    class Adapter : JsonSerializer<CombatLogEntry>, JsonDeserializer<CombatLogEntry> {
        override fun serialize(entry: CombatLogEntry, type: Type, context: JsonSerializationContext): JsonElement {
            TODO("Not yet implemented")
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): CombatLogEntry {
            TODO("Not yet implemented")
        }
    }
}
