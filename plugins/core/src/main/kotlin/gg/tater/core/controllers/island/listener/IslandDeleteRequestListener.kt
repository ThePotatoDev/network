package gg.tater.core.controllers.island.listener

import com.google.common.cache.LoadingCache
import gg.tater.shared.island.Island
import gg.tater.shared.island.message.IslandDeleteRequest
import gg.tater.shared.network.server.ServerDataService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit

class IslandDeleteRequestListener(
    private val cache: LoadingCache<String, Island>,
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