package gg.tater.shared.island.flag.model

import me.lucko.helper.terminable.module.TerminableModule

interface IslandFlagHandler : TerminableModule {

    fun type(): FlagType

}