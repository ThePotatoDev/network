package gg.tater.core.island.experience.stage

import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

data class ExperienceStage(
    val id: Int,
    val displayPrompts: Array<Component>,
    val controller: TerminableModule,
    val meta: MutableMap<String, String> = mutableMapOf(),
) {

    fun sendDisplayPrompts(player: Player) {
        displayPrompts.forEach { player.sendMessage(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExperienceStage

        if (id != other.id) return false
        if (!displayPrompts.contentEquals(other.displayPrompts)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + displayPrompts.contentHashCode()
        return result
    }
}
