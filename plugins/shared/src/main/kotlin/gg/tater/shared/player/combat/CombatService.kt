package gg.tater.shared.player.combat

import org.bukkit.entity.Player
import java.util.*

interface CombatService {

    fun isInCombat(uuid: UUID): Boolean

    fun setInCombat(uuid: UUID): Boolean

    fun spawnCombatNPC(player: Player)

}