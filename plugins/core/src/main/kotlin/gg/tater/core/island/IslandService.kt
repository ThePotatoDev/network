package gg.tater.core.island

import gg.tater.core.annotation.InvocationContext
import gg.tater.core.annotation.InvocationContextType
import gg.tater.core.island.player.IslandPlayer
import gg.tater.core.server.model.GameModeType
import gg.tater.core.server.model.ServerDataModel
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.entity.Player
import org.redisson.api.RFuture
import org.redisson.api.RMap
import java.util.*

abstract class IslandService<T : Island, K : IslandPlayer>(val mode: GameModeType) : TerminableModule {

    abstract fun getIslandFor(player: K): RFuture<T?>?

    abstract fun getIsland(islandId: UUID): RFuture<T?>

    abstract fun save(island: T): RFuture<Boolean>

    abstract fun all(): RFuture<Collection<T>>

    abstract fun hasInvite(uuid: UUID, island: T): RFuture<Boolean>

    abstract fun addInvite(uuid: UUID, island: T): RFuture<Boolean>

    abstract fun createFor(player: K, server: ServerDataModel)

    abstract fun transaction(
        operation: (RMap<UUID, T>) -> Unit,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    )

    @InvocationContext(InvocationContextType.ASYNC)
    abstract fun directToOccupiedServer(sender: Player, island: T)
}