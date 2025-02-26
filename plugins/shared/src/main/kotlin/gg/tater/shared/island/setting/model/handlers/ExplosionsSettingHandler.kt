package gg.tater.shared.island.setting.model.handlers

import gg.tater.shared.island.cache.IslandWorldCacheService
import gg.tater.shared.island.setting.model.IslandSettingHandler
import gg.tater.shared.island.setting.model.IslandSettingType
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent

class ExplosionsSettingHandler : IslandSettingHandler() {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(BlockExplodeEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val block = it.block
                val world = block.world
                val cache = Services.load(IslandWorldCacheService::class.java)
                val island = cache.getIsland(world) ?: return@handler

                val enabled = island.isSettingEnabled(IslandSettingType.EXPLOSIONS)
                if (enabled) return@handler

                it.isCancelled = true
                it.blockList().clear()
            }
            .bindWith(consumer)

        Events.subscribe(EntityExplodeEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val entity = it.entity
                val world = entity.world
                val cache = Services.load(IslandWorldCacheService::class.java)
                val island = cache.getIsland(world) ?: return@handler

                val enabled = island.isSettingEnabled(IslandSettingType.EXPLOSIONS)
                if (enabled) return@handler

                it.isCancelled = true
                it.blockList().clear()
            }
            .bindWith(consumer)
    }
}