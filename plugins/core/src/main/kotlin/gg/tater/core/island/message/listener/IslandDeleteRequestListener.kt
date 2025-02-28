package gg.tater.core.island.message.listener

import gg.tater.core.island.Island
import gg.tater.core.island.cache.IslandWorldCacheService
import gg.tater.core.island.message.IslandDeleteRequest
import gg.tater.core.redis.Redis
import gg.tater.core.server.ServerDataService
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit

class IslandDeleteRequestListener<T : Island>(
    private val cache: IslandWorldCacheService<T> = Services.load(IslandWorldCacheService::class.java) as IslandWorldCacheService<T>,
    private val redis: Redis = Services.load(Redis::class.java),
    private val server: String = Services.load(ServerDataService::class.java).id()
) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        redis.listen<IslandDeleteRequest> {
            // Make sure the server name matches, ignore it otherwise
            if (it.server == null || it.server != server) return@listen
            val id = it.islandId.toString()

            val world = Bukkit.getWorld(id) ?: return@listen
            cache.invalidate(id)

            // Send all players on the island to spawn
            Schedulers.sync().run {
                for (player in world.players) {
                    player.performCommand("spawn")
                }
            }

            Schedulers.sync().runLater({
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "swm unload $id")
            }, 20L * 3L)
        }
    }
}