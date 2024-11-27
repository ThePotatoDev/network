package gg.tater.shared.island.setting.model.handlers

import gg.tater.shared.island.IslandService
import gg.tater.shared.island.setting.model.IslandSettingHandler
import gg.tater.shared.island.setting.model.IslandSettingType
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockIgniteEvent

class FireSpreadSettingHandler(service: IslandService) : IslandSettingHandler(service) {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(BlockIgniteEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val block = it.ignitingBlock ?: return@handler
                val world = block.location.world
                val island = service.getIsland(world) ?: return@handler

                val enabled = island.isSettingEnabled(IslandSettingType.FIRE_SPREAD)
                if (enabled) return@handler

                it.isCancelled = true
            }
            .bindWith(consumer)
    }
}