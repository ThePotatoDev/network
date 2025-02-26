package gg.tater.shared.island.message.listener

import com.google.common.cache.LoadingCache
import gg.tater.shared.island.Island
import gg.tater.shared.island.cache.IslandWorldCacheService
import gg.tater.shared.island.message.IslandUpdateRequest
import gg.tater.shared.server.ServerDataService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

class IslandUpdateRequestListener(
    private val cache: IslandWorldCacheService<*> = Services.load(IslandWorldCacheService::class.java),
    private val redis: Redis = Services.load(Redis::class.java),
    private val server: String = Services.load(ServerDataService::class.java).id()
) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        redis.listen<IslandUpdateRequest> {
            // Make sure the server name matches, ignore it otherwise
            if (it.server == null || it.server != server) return@listen
            val id = it.islandId.toString()
            cache.refresh(id)
            println("Handled refresh island request: $id")
        }
    }
}