package gg.tater.core.controllers.island.listener

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.api.loaders.SlimeLoader
import com.infernalsuite.aswm.api.world.SlimeWorld
import gg.tater.core.controllers.island.IslandController.Companion.PROPERTIES
import gg.tater.shared.redis.Redis
import gg.tater.shared.island.message.placement.IslandPlacementRequest
import gg.tater.shared.island.message.placement.IslandPlacementResponse
import gg.tater.shared.network.model.ServerType
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.Instant

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

            val id = it.islandId
            val worldName = id.toString()
            val exists = loader.worldExists(worldName)

            val island = redis.islands()[id]!!
            island.lastActivity = Instant.now()
            redis.islands().fastPut(id, island)

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
            if (player == null) {
                redis.publish(IslandPlacementResponse(server, it.playerId, worldName, it.internal))
                return@listen
            }

            val spawn = ServerType.SERVER.spawn

            // If the player is on the same server teleport them to spawn location
            player.teleportAsync(
                Location(
                    Bukkit.getWorld(worldName),
                    spawn.x,
                    spawn.y,
                    spawn.z,
                    spawn.yaw,
                    spawn.pitch
                )
            )
        }
    }
}