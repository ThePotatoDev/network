package gg.tater.core.island.experience.stage

import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component

data class ExperienceStage(
    val id: Int,
    val startPrompts: Array<Component>,
    val endPrompts: Array<Component>,
    val controller: TerminableModule,
    val meta: MutableMap<String, String> = mutableMapOf(),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExperienceStage

        if (id != other.id) return false
        if (!startPrompts.contentEquals(other.startPrompts)) return false
        if (!endPrompts.contentEquals(other.endPrompts)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + startPrompts.contentHashCode()
        result = 31 * result + endPrompts.contentHashCode()
        return result
    }
}
