package gg.tater.shared.island.setting.model.handlers

import gg.tater.shared.island.IslandService
import gg.tater.shared.island.setting.model.IslandSettingHandler
import gg.tater.shared.island.setting.model.IslandSettingType
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.event.EventPriority
import org.bukkit.event.weather.WeatherChangeEvent

class RainingSettingHandler(service: IslandService) : IslandSettingHandler(service) {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(WeatherChangeEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val world = it.world
                val island = service.getIsland(world) ?: return@handler

                val value = island.getSettingValue(IslandSettingType.RAINING)
                if (value) return@handler

                world.setStorm(false)
                it.isCancelled = true
            }
            .bindWith(consumer)
    }
}