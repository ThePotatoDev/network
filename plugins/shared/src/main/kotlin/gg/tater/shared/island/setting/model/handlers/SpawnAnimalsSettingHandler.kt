package gg.tater.shared.island.setting.model.handlers

import gg.tater.shared.island.cache.IslandWorldCacheService
import gg.tater.shared.island.setting.model.IslandSettingHandler
import gg.tater.shared.island.setting.model.IslandSettingType
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.entity.Animals
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntitySpawnEvent

class SpawnAnimalsSettingHandler : IslandSettingHandler() {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(EntitySpawnEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.entity is Animals }
            .handler {
                val world = it.location.world
                val cache = Services.load(IslandWorldCacheService::class.java)
                val island = cache.getIsland(world) ?: return@handler

                val enabled = island.isSettingEnabled(IslandSettingType.SPAWN_ANIMALS)
                if (enabled) return@handler

                it.isCancelled = true
            }
            .bindWith(consumer)
    }
}