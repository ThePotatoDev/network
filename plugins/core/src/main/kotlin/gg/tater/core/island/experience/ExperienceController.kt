package gg.tater.core.island.experience

import gg.tater.core.island.Island
import gg.tater.core.island.experience.player.ExperiencePlayerController
import gg.tater.core.island.experience.player.ExperiencePlayerService
import gg.tater.core.island.experience.stage.ExperienceStage
import gg.tater.core.server.model.GameModeType
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.entity.Player

class ExperienceController<T : Island>(private val mode: GameModeType) : ExperienceService<T> {

    private val stages: MutableMap<Int, ExperienceStage> = mutableMapOf()

    override fun startExperience(player: Player): Boolean {
        val start = stages[0] ?: return false

        for (prompt in start.startPrompts) {
            player.sendMessage(prompt)
        }

        return true
    }

    override fun addStage(stage: ExperienceStage) {
        stages[stage.id] = stage
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(ExperiencePlayerService::class.java, consumer.bindModule(ExperiencePlayerController(mode)))

        for (stage in stages.values) {
            consumer.bindModule(stage.controller)
        }
    }
}