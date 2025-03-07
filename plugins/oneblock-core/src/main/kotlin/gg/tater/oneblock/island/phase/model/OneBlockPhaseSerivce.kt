package gg.tater.oneblock.island.phase.model

import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.entity.Player

interface OneBlockPhaseSerivce : TerminableModule {

    fun all(): Collection<OneBlockPhase>

    fun default(): OneBlockPhase

    fun getById(id: Int): OneBlockPhase

    fun getNextPhase(current: OneBlockPhase): OneBlockPhase?

    fun getProgressiveRewards(phase: OneBlockPhase, count: Int): List<String>?

    fun dispatchProgressiveRewards(player: Player, rewards: List<String>)

}