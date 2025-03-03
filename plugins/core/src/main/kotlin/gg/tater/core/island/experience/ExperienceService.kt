package gg.tater.core.island.experience

import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.concurrent.CompletionStage

interface ExperienceService : TerminableModule {

    fun startExperience(player: Player): CompletionStage<Void>

    fun sendExperienceMessage(messages: Component, player: Player) {
        arrayOf(Component.empty(), message, Component.empty()).forEach { player.sendMessage(it) }
    }
}