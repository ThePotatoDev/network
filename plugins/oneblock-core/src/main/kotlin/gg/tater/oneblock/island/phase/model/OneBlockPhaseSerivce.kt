package gg.tater.oneblock.island.phase.model

import me.lucko.helper.terminable.module.TerminableModule

interface OneBlockPhaseSerivce : TerminableModule {

    fun all(): Collection<OneBlockPhase>

    fun default(): OneBlockPhase

    fun getById(id: Int): OneBlockPhase

}