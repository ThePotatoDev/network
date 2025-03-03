package gg.tater.core.island.experience

import gg.tater.core.island.Island
import gg.tater.core.island.experience.stage.ExperienceStage
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.entity.Player

interface ExperienceService<T : Island> : TerminableModule {

    fun startExperience(player: Player): Boolean

    fun addStage(stage: ExperienceStage)

}