package gg.tater.core.island.message.listener

import gg.tater.core.island.Island
import gg.tater.core.island.cache.IslandWorldCacheService
import gg.tater.core.island.message.IslandUpdateRequest
import gg.tater.core.redis.Redis
import gg.tater.core.server.ServerDataService
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

class IslandUpdateRequestListener<T : Island>(
    private val cache: IslandWorldCacheService<T> = Services.load(IslandWorldCacheService::class.java) as IslandWorldCacheService<T>,
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