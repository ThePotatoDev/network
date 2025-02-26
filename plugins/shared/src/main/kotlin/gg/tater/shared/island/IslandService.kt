package gg.tater.shared.island

import gg.tater.shared.annotation.InvocationContext
import gg.tater.shared.annotation.InvocationContextType
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.server.model.GameModeType
import gg.tater.shared.server.model.ServerDataModel
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.entity.Player
import org.redisson.api.RFuture
import org.redisson.api.RMap
import java.util.*

abstract class IslandService<T : Island>(val mode: GameModeType) : TerminableModule {

    abstract fun getIslandFor(player: PlayerDataModel): RFuture<T?>?

    abstract fun getIsland(islandId: UUID): RFuture<T?>

    abstract fun save(island: T): RFuture<Boolean>

    abstract fun all(): RFuture<Collection<T>>

    abstract fun hasInvite(uuid: UUID, island: T): RFuture<Boolean>

    abstract fun addInvite(uuid: UUID, island: T): RFuture<Boolean>

    abstract fun createFor(player: PlayerDataModel, server: ServerDataModel)

    abstract fun transaction(
        operation: (RMap<UUID, T>) -> Unit,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    )

    @InvocationContext(InvocationContextType.ASYNC)
    abstract fun directToOccupiedServer(sender: Player, island: T)
}