package gg.tater.shared.island.flag.model.handlers

import gg.tater.shared.island.flag.model.FlagType
import gg.tater.shared.island.flag.model.IslandFlagHandler
import gg.tater.shared.island.IslandService
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class UsePressurePlatesFlagHandler : IslandFlagHandler {

    override fun type(): FlagType {
        return FlagType.USE_PRESSURE_PLATES
    }

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.action == Action.PHYSICAL }
            .filter { it.interactionPoint != null && it.interactionPoint!!.block.type.name.contains("PRESSURE_PLATE") }
            .handler {
                val player = it.player
                val world = player.world
                val islands = Services.load(IslandService::class.java)
                val island = islands.getIsland(world) ?: return@handler

                if (island.canInteract(player.uniqueId, FlagType.USE_PRESSURE_PLATES)) return@handler

                it.isCancelled = true
                player.sendMessage(Component.text("You cannot do that on this island!", NamedTextColor.RED))
            }
            .bindWith(consumer)
    }
}