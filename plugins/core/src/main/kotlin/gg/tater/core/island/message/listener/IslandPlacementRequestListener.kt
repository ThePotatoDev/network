package gg.tater.core.island.message.listener

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.api.loaders.SlimeLoader
import com.infernalsuite.aswm.api.world.SlimeWorld
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap
import gg.tater.core.island.Island
import gg.tater.core.island.IslandService
import gg.tater.core.island.event.IslandPlacementEvent
import gg.tater.core.island.message.placement.IslandPlacementRequest
import gg.tater.core.island.message.placement.IslandPlacementResponse
import gg.tater.core.island.player.IslandPlayer
import gg.tater.core.redis.Redis
import gg.tater.core.server.ServerDataService
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit
import java.time.Instant
import java.util.concurrent.TimeUnit

class IslandPlacementRequestListener<T : Island, K : IslandPlayer>(
    private val loader: SlimeLoader,
    private val template: SlimeWorld,
    private val properties: SlimePropertyMap,
    private val redis: Redis = Services.load(Redis::class.java),
    private val server: String = Services.load(ServerDataService::class.java).id()
) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        val api = AdvancedSlimePaperAPI.instance()

        redis.listen<IslandPlacementRequest> {
            // Make sure the server name matches, ignore it otherwise
            if (it.server != server) return@listen

            val islands: IslandService<T, K> = Services.load(IslandService::class.java) as IslandService<T, K>

            /**
             * Acquire a semaphore for this server with a
             * lease of 3 seconds and waits up to 3 seconds
             */
            val semaphoreId = redis.semaphores(server).tryAcquire(3L, 3L, TimeUnit.SECONDS)

            if (semaphoreId != null) {
                val id = it.islandId
                val worldName = id.toString()
                val exists = loader.worldExists(worldName)

                val island = islands.getIsland(id).get()!!
                island.lastActivity = Instant.now()
                islands.save(island)

                val world: SlimeWorld = if (exists) {
                    api.readWorld(loader, worldName, false, properties)
                } else {
                    template.clone(worldName, loader)
                }

                Schedulers.sync().run {
                    // Load the world if it doesn't exist
                    if (Bukkit.getWorld(worldName) == null) {
                        Events.callSync(IslandPlacementEvent(it.playerId, island, api.loadWorld(world, false)))
                    }
                }

                val player = Bukkit.getPlayer(it.playerId)

                // If player is not on the server, send a network request to place them on the island server
                // Island placement semaphore is unlocked in the player controller when their login is handled
                if (player == null) {
                    redis.publish(IslandPlacementResponse(server, it.playerId, worldName, it.internal))
                    return@listen
                }

                //TODO: handle teleport if on matching server already
            }
        }
    }
}