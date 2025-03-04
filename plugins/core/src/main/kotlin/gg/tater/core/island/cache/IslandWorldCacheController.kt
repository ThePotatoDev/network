package gg.tater.core.island.cache

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import gg.tater.core.UUID_REGEX
import gg.tater.core.annotation.Controller
import gg.tater.core.island.Island
import gg.tater.core.island.IslandService
import gg.tater.core.island.event.IslandUnloadEvent
import gg.tater.core.island.player.IslandPlayer
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Bukkit
import org.bukkit.World
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@Controller(
    id = "island-world-cache-controller"
)
class IslandWorldCacheController<T : Island, K : IslandPlayer> : IslandWorldCacheService<T> {

    private companion object {
        val SCHEDULER: ScheduledExecutorService = Executors.newScheduledThreadPool(5)
    }

    /**
     * Store islands temporarily when needed in runtime by world name for usage
     * on main thread without impacting main thread continually w/redis actions
     */
    private val cache = CacheBuilder.newBuilder()
        .refreshAfterWrite(Duration.ofMinutes(1L))
        .build(CacheLoader.asyncReloading(object : CacheLoader<String, T>() {
            override fun load(worldName: String): T {
                val service: IslandService<T, K> = Services.load(IslandService::class.java) as IslandService<T, K>
                val islandId = UUID.fromString(worldName)
                return service.getIsland(islandId).get()!!
            }
        }, SCHEDULER))

    override fun getIsland(world: World): T? {
        val name = world.name
        if (!world.isIslandWorld()) return null
        return cache.get(name)
    }

    override fun refresh(id: String) {
        cache.refresh(id)
    }

    override fun invalidate(id: String) {
        cache.invalidate(id)
    }

    override fun setup(consumer: TerminableConsumer) {
        val api = AdvancedSlimePaperAPI.instance()

        Schedulers.async().runRepeating(Runnable {
            val service: IslandService<T, K> = Services.load(IslandService::class.java) as IslandService<T, K>

            for (world in api.loadedWorlds) {
                val worldName = world.name
                if (!UUID_REGEX.matches(worldName)) continue

                val islandId = UUID.fromString(worldName)
                val island = service.getIsland(islandId).get() ?: continue
                val lastActive = island.lastActivity

                // If 30 seconds of inactivity have not passed, continue to next
                if (Instant.now().isBefore(lastActive.plusSeconds(30L))) continue

                val bukkitWorld = Bukkit.getWorld(worldName) ?: continue
                val empty = bukkitWorld.players.size <= 0

                island.lastActivity = Instant.now()
                service.save(island)

                // If the island still has players present on it, keep it loaded
                if (!empty) continue

                Events.call(IslandUnloadEvent(island, world))
                Schedulers.sync().run { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "swm unload $worldName") }
                println("Unloading island $worldName due to inactivity.")
            }
        }, 20L, 20L).bindWith(consumer)

        Services.provide(IslandWorldCacheService::class.java, this)
    }
}