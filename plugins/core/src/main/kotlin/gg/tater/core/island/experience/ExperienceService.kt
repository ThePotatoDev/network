package gg.tater.core.island.experience

import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

interface ExperienceService : TerminableModule {

    fun startExperience(player: Player)

    fun sendExperienceMessage(message: Component, player: Player) {
        arrayOf(Component.empty(), message, Component.empty()).forEach { player.sendMessage(it) }
    }
}