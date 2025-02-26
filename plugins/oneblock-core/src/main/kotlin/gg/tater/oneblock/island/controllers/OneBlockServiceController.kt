package gg.tater.oneblock.island.controllers

import gg.tater.oneblock.island.OneBlockIsland
import gg.tater.shared.annotation.Controller
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.message.placement.IslandPlacementRequest
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.redis.Redis
import gg.tater.shared.redis.transactional
import gg.tater.shared.server.model.GameModeType
import gg.tater.shared.server.model.ServerDataModel
import gg.tater.shared.server.model.ServerType
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.redisson.api.RFuture
import org.redisson.api.RMap
import java.util.*

@Controller(
    id = "oneblock-service-controller"
)
class OneBlockServiceController : IslandService<OneBlockIsland>(GameModeType.ONEBLOCK) {

    private companion object {
        val ISLAND_MAP_NAME = "${GameModeType.ONEBLOCK.id}_islands"
        val ISLAND_INVITES_MAP_NAME = "${GameModeType.ONEBLOCK.id}_island_invites"
    }

    private val redis = Services.load(Redis::class.java)

    override fun all(): RFuture<Collection<OneBlockIsland>> {
        return Services.load(Redis::class.java).client.getMap<UUID, OneBlockIsland>(ISLAND_MAP_NAME)
            .readAllValuesAsync()
    }

    override fun getIslandFor(player: PlayerDataModel): RFuture<OneBlockIsland?>? {
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

    override fun createFor(player: PlayerDataModel, server: ServerDataModel) {
        val newIsland = OneBlockIsland(UUID.randomUUID(), player.uuid, player.name)
        newIsland.currentServerId = server.id
        save(newIsland)

        val players = Services.load(PlayerService::class.java)
        player.islandId = newIsland.id
        player.setDefaultSpawn(ServerType.ONEBLOCK_SERVER)

        players.transaction(
            player.setPositionResolver(PlayerPositionResolver.Type.TELEPORT_ISLAND_HOME),
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
        Services.provide(IslandService::class.java, this)
    }
}