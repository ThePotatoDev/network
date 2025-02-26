package gg.tater.shared.island.setting.model.handlers

import gg.tater.shared.island.cache.IslandWorldCacheService
import gg.tater.shared.island.setting.model.IslandSettingHandler
import gg.tater.shared.island.setting.model.IslandSettingType
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.event.EventPriority
import org.bukkit.event.weather.WeatherChangeEvent

class RainingSettingHandler : IslandSettingHandler() {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(WeatherChangeEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val world = it.world
                val cache = Services.load(IslandWorldCacheService::class.java)
                val island = cache.getIsland(world) ?: return@handler

                val value = island.getSettingValue(IslandSettingType.RAINING)
                if (value) return@handler

                world.setStorm(false)
                it.isCancelled = true
            }
            .bindWith(consumer)
    }
}