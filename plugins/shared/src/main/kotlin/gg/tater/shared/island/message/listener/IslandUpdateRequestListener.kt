package gg.tater.shared.island.message.listener

import gg.tater.shared.island.Island
import gg.tater.shared.island.cache.IslandWorldCacheService
import gg.tater.shared.island.message.IslandUpdateRequest
import gg.tater.shared.redis.Redis
import gg.tater.shared.server.ServerDataService
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