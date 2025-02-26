package gg.tater.shared.island

import gg.tater.shared.annotation.InvocationContext
import gg.tater.shared.annotation.InvocationContextType
import gg.tater.shared.island.message.placement.IslandPlacementRequest
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.redis.Redis
import gg.tater.shared.redis.transactional
import gg.tater.shared.server.model.GameModeType
import gg.tater.shared.server.model.ServerDataModel
import gg.tater.shared.server.model.ServerType
import me.lucko.helper.Services
import me.lucko.helper.promise.ThreadContext
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
    fun directToOccupiedServer(sender: Player, island: T) {
        if (ThreadContext.forCurrentThread() != ThreadContext.ASYNC) {
            throw IllegalStateException("This method must be called asynchronously.")
        }

        val redis = Services.load(Redis::class.java)

        // If the island is already placed on a server, teleport the player to the server
        val currentServerId = island.currentServerId
        var server: ServerDataModel?

        if (currentServerId != null) {
            server = redis.getServer(currentServerId).get()

            // If the server is not online, place the island on a fresh server
            if (server == null) {
                server = redis.getServer(ServerType.SERVER) ?: return
            }
        } else {
            // If everything else fails to check, place the island on a fresh server
            server = redis.getServer(ServerType.SERVER) ?: return
        }

        redis.publish(IslandPlacementRequest.of(sender, island, server))
        island.currentServerId = server.id
        save(island)
    }
}