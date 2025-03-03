package gg.tater.oneblock.island.controllers

import gg.tater.core.annotation.Controller
import gg.tater.core.island.IslandService
import gg.tater.core.island.message.placement.IslandPlacementRequest
import gg.tater.core.island.player.position.PositionDirector
import gg.tater.core.island.player.position.SpawnPositionData
import gg.tater.core.redis.Redis
import gg.tater.core.redis.transactional
import gg.tater.core.server.model.GameModeType
import gg.tater.core.server.model.ServerDataModel
import gg.tater.core.server.model.ServerType
import gg.tater.oneblock.island.OneBlockIsland
import gg.tater.oneblock.player.OneBlockPlayer
import gg.tater.oneblock.player.OneBlockPlayerService
import me.lucko.helper.Services
import me.lucko.helper.promise.ThreadContext
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.redisson.api.RFuture
import org.redisson.api.RMap
import java.util.*

@Controller(
    id = "oneblock-service-controller"
)
class OneBlockIslandService : IslandService<OneBlockIsland, OneBlockPlayer>(GameModeType.ONEBLOCK) {

    private companion object {
        val ISLAND_MAP_NAME = "${GameModeType.ONEBLOCK.id}_islands"
        val ISLAND_INVITES_MAP_NAME = "${GameModeType.ONEBLOCK.id}_island_invites"
    }

    private val redis = Services.load(Redis::class.java)

    override fun all(): RFuture<Collection<OneBlockIsland>> {
        return Services.load(Redis::class.java).client.getMap<UUID, OneBlockIsland>(ISLAND_MAP_NAME)
            .readAllValuesAsync()
    }

    override fun getIslandFor(player: OneBlockPlayer): RFuture<OneBlockIsland?>? {
        if (player.islandId == null) return null
        return redis.client.getMap<UUID, OneBlockIsland>(ISLAND_MAP_NAME)
            .getAsync(player.islandId)
    }

    override fun getIsland(islandId: UUID): RFuture<OneBlockIsland?> {
        return redis.client.getMap<UUID, OneBlockIsland>(ISLAND_MAP_NAME)
            .getAsync(islandId)
    }

    override fun save(island: OneBlockIsland): RFuture<Boolean> {
        return redis.client.getMap<UUID, OneBlockIsland>(ISLAND_MAP_NAME)
            .fastPutAsync(island.id, island)
    }

    override fun hasInvite(uuid: UUID, island: OneBlockIsland): RFuture<Boolean> {
        return redis.client.getListMultimapCache<UUID, UUID>(ISLAND_INVITES_MAP_NAME)
            .containsEntryAsync(uuid, uuid)
    }

    override fun addInvite(uuid: UUID, island: OneBlockIsland): RFuture<Boolean> {
        return redis.client.getListMultimapCache<UUID, UUID>(ISLAND_INVITES_MAP_NAME)
            .putAsync(uuid, uuid)
    }

    override fun createFor(player: OneBlockPlayer, server: ServerDataModel) {
        val newIsland = OneBlockIsland(UUID.randomUUID(), player.uuid, player.name)
        newIsland.currentServerId = server.id
        save(newIsland)

        val players = Services.load(OneBlockPlayerService::class.java)

        player.islandId = newIsland.id

        player.setServerSpawnPos(
            ServerType.ONEBLOCK_SERVER,
            PositionDirector.ISLAND_TELEPORT_DIRECTOR,
            newIsland.spawn,
            mutableMapOf(SpawnPositionData.ISLAND_ID_META_KEY to newIsland.id.toString())
        )

        players.transaction(
            player,
            onSuccess = {
                redis.publish(
                    IslandPlacementRequest(
                        server.id,
                        player.uuid,
                        newIsland.id,
                        player.name,
                        true
                    )
                )
            })
    }

    override fun directToOccupiedServer(sender: Player, island: OneBlockIsland) {
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
                server = redis.getServer(ServerType.ONEBLOCK_SERVER) ?: return
            }
        } else {
            // If everything else fails to check, place the island on a fresh server
            server = redis.getServer(ServerType.ONEBLOCK_SERVER) ?: return
        }

        redis.publish(IslandPlacementRequest.of(sender, island, server))
        island.currentServerId = server.id
        save(island)
    }

    override fun transaction(
        operation: (RMap<UUID, OneBlockIsland>) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        redis.client.apply {
            this.getMap<UUID, OneBlockIsland>(ISLAND_MAP_NAME).transactional(operation, onSuccess, onFailure)
        }
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(OneBlockIslandService::class.java, this)
        Services.provide(IslandService::class.java, this)
    }
}