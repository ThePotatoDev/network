package gg.tater.core.island.setting.model.handlers

import gg.tater.core.island.cache.IslandWorldCacheService
import gg.tater.core.island.setting.model.IslandSettingHandler
import gg.tater.core.island.setting.model.IslandSettingType
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockIgniteEvent

class FireSpreadSettingHandler : IslandSettingHandler() {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(BlockIgniteEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val block = it.ignitingBlock ?: return@handler
                val world = block.location.world
                val cache = Services.load(IslandWorldCacheService::class.java)
                val island = cache.getIsland(world) ?: return@handler

                val enabled = island.isSettingEnabled(IslandSettingType.FIRE_SPREAD)
                if (enabled) return@handler

                it.isCancelled = true
            }
            .bindWith(consumer)
    }
}