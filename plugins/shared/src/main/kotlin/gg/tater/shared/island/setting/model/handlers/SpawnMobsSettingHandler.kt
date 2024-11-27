package gg.tater.shared.island.setting.model.handlers

import gg.tater.shared.island.IslandService
import gg.tater.shared.island.setting.model.IslandSettingHandler
import gg.tater.shared.island.setting.model.IslandSettingType
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.entity.Mob
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntitySpawnEvent

class SpawnMobsSettingHandler(service: IslandService) : IslandSettingHandler(service) {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(EntitySpawnEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.entity is Mob }
            .handler {
                val world = it.location.world
                val island = service.getIsland(world) ?: return@handler

                val enabled = island.isSettingEnabled(IslandSettingType.SPAWN_MOBS)
                if (enabled) return@handler

                it.isCancelled = true
            }
            .bindWith(consumer)
    }
}