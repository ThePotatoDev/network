package gg.tater.shared.island.flag.model.handlers

import gg.tater.shared.island.cache.IslandWorldCacheService
import gg.tater.shared.island.flag.model.FlagType
import gg.tater.shared.island.flag.model.IslandFlagHandler
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent

class BreakSpawnersFlagHandler : IslandFlagHandler {

    override fun type(): FlagType {
        return FlagType.BREAK_SPAWNERS
    }

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(BlockBreakEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                val world = player.world
                val cache = Services.load(IslandWorldCacheService::class.java)
                val island = cache.getIsland(world) ?: return@handler
                val block = it.block

                if (!block.type.name.contains("SPAWNER")) return@handler
                if (island.canInteract(player.uniqueId, FlagType.BREAK_SPAWNERS)) return@handler

                it.isCancelled = true
                player.sendMessage(Component.text("You cannot do that on this island!", NamedTextColor.RED))
            }
            .bindWith(consumer)
    }
}