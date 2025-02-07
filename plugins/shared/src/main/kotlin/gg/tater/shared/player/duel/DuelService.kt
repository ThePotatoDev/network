package gg.tater.shared.player.duel

import gg.tater.shared.player.duel.model.DuelRequest
import me.lucko.helper.terminable.module.TerminableModule
import java.util.*

interface DuelService : TerminableModule {

    fun startDuel(request: DuelRequest)

    fun endDuel(request: DuelRequest)

    fun isInDuel(uuid: UUID): Boolean

}