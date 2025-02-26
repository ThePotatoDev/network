package gg.tater.shared.island

import gg.tater.shared.annotation.InvocationContext
import gg.tater.shared.annotation.InvocationContextType
import gg.tater.shared.network.server.ServerDataModel
import gg.tater.shared.player.PlayerDataModel
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.World
import org.bukkit.entity.Player
import org.redisson.api.RFuture
import org.redisson.api.RMap
import java.util.*

interface IslandService : TerminableModule {

    fun getIsland(world: World): Island?

    fun getIslandFor(player: PlayerDataModel): RFuture<Island?>?

    fun getIsland(islandId: UUID): RFuture<Island?>

    fun save(island: Island): RFuture<Boolean>

    fun all(): RFuture<Collection<Island>>

    fun hasInvite(uuid: UUID, island: Island): RFuture<Boolean>

    fun addInvite(uuid: UUID, island: Island): RFuture<Boolean>

    fun createFor(player: PlayerDataModel, server: ServerDataModel)

    @InvocationContext(InvocationContextType.ASYNC)
    fun directToOccupiedServer(sender: Player, island: Island): Boolean

    fun transaction(
        operation: (RMap<UUID, Island>) -> Unit,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    )

    companion object {
        const val ISLAND_MAP_NAME = "islands"
        const val ISLAND_INVITES_MAP_NAME = "island_invites"
    }

}