package gg.tater.core.controllers.island.listener

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.api.loaders.SlimeLoader
import com.infernalsuite.aswm.api.world.SlimeWorld
import gg.tater.core.controllers.island.IslandController.Companion.PROPERTIES
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.message.placement.IslandPlacementRequest
import gg.tater.shared.island.message.placement.IslandPlacementResponse
import gg.tater.shared.network.model.server.ServerType
import gg.tater.shared.redis.Redis
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant
import java.util.concurrent.TimeUnit

class IslandPlacementRequestListener(
    private val redis: Redis,
    private val server: String,
    private val api: AdvancedSlimePaperAPI,
    private val loader: SlimeLoader,
    private val template: SlimeWorld
) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        redis.listen<IslandPlacementRequest> {
            // Make sure the server name matches, ignore it otherwise
            if (it.server != server) return@listen

            val islands: IslandService = Services.load(IslandService::class.java)

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
                    api.readWorld(loader, worldName, false, PROPERTIES)
                } else {
                    template.clone(worldName, loader)
                }

                Schedulers.sync().run {
                    // Load the world if it doesn't exist
                    if (Bukkit.getWorld(worldName) == null) {
                        api.loadWorld(world, false)
                    }
                }

                val player = Bukkit.getPlayer(it.playerId)

                // If player is not on the server, send a network request to place them on the island server
                // Island placement semaphore is unlocked in the player controller when their login is handled
                if (player == null) {
                    redis.publish(IslandPlacementResponse(server, it.playerId, worldName, it.internal))
                    return@listen
                }

                val spawn = ServerType.SERVER.spawn

                // If the player is on the same server teleport them to spawn location
                player.teleportAsync(
                    Location(
                        Bukkit.getWorld(worldName),
                        spawn!!.x,
                        spawn.y,
                        spawn.z,
                        spawn.yaw,
                        spawn.pitch
                    )
                )
            }
        }
    }
}