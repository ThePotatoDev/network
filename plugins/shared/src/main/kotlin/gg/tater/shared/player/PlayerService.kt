package gg.tater.shared.player

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface PlayerService : TerminableModule {

    companion object {
        const val PLAYER_MAP_NAME = "players"
    }

    fun compute(name: String, uuid: UUID): RFuture<PlayerDataModel>

    fun get(uuid: UUID): RFuture<PlayerDataModel>

    fun save(data: PlayerDataModel): RFuture<Boolean>

    fun transaction(data: PlayerDataModel, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})

}