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
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class UseButtonsFlagHandler : IslandFlagHandler {

    override fun type(): FlagType {
        return FlagType.USE_BUTTOMS
    }

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.action == Action.RIGHT_CLICK_BLOCK }
            .filter { it.clickedBlock != null && it.clickedBlock!!.type.name.contains("BUTTON") }
            .handler {
                val player = it.player
                val world = player.world
                val cache = Services.load(IslandWorldCacheService::class.java)
                val island = cache.getIsland(world) ?: return@handler

                if (island.canInteract(player.uniqueId, FlagType.USE_BUTTOMS)) return@handler

                it.isCancelled = true
                player.sendMessage(Component.text("You cannot do that on this island!", NamedTextColor.RED))
            }
            .bindWith(consumer)
    }
}