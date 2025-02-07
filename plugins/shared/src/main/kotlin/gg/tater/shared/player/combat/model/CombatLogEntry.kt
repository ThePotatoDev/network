package gg.tater.shared.player.combat.model

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.util.*

data class CombatLogEntry(val uuid: UUID, val inventory: Inventory) {

    constructor(player: Player) : this(player.uniqueId, player.inventory)

}
