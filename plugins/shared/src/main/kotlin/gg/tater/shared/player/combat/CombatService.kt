package gg.tater.shared.player.combat

import de.oliver.fancynpcs.api.Npc
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.entity.Player
import java.util.*

interface CombatService : TerminableModule {

    fun isInCombat(uuid: UUID): Boolean

    fun setInCombat(uuid: UUID): Boolean

    fun spawnCombatNPC(player: Player): Npc

}