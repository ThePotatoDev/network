package gg.tater.core.controllers.server.hub

import gg.tater.shared.annotation.Controller
import gg.tater.shared.server.model.ServerType
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.inventory.ItemStack

@Controller(
    id = "hub-controller",
    ignoredBinds = [
        ServerType.SERVER,
        ServerType.SPAWN,
        ServerType.PVP,
        ServerType.PLANET
    ]
)
class HubController : TerminableModule {

    private companion object {
        val SPAWN_LOCATION = ServerType.HUB.spawn!!.let {
            Location(
                Bukkit.getWorld("world"),
                it.x,
                it.y,
                it.z,
                it.yaw,
                it.pitch
            )
        }

        val SERVER_ITEM_BUILDER: ItemStack = ItemStackBuilder.of(Material.CLOCK)
            .name("&aServer Menu &7(Right-Click)")
            .lore("&7&oRight-Click to join a server!")
            .build()
    }

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val action = it.action
                if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return@handler
                val hand = it.hand ?: return@handler
                val stack = it.player.inventory.getItem(hand)
                if (stack != SERVER_ITEM_BUILDER) return@handler
                HubServerGui(it.player).open()
            }
            .bindWith(consumer)

        Events.subscribe(PlayerJoinEvent::class.java)
            .handler {
                val player = it.player

                it.joinMessage(null)

                Schedulers.sync().runLater({
                    player.teleportAsync(SPAWN_LOCATION)
                    player.health = 20.0
                    player.foodLevel = 20
                    player.gameMode = GameMode.ADVENTURE

                    player.inventory.setItem(4, SERVER_ITEM_BUILDER)
                }, 2L)
            }
            .bindWith(consumer)

        Events.subscribe(PlayerQuitEvent::class.java)
            .handler {
                it.quitMessage(null)
            }
            .bindWith(consumer)

        Events.subscribe(WeatherChangeEvent::class.java)
            .handler {
                it.isCancelled = true
            }
            .bindWith(consumer)

        Events.subscribe(FoodLevelChangeEvent::class.java)
            .handler {
                it.isCancelled = true
            }
            .bindWith(consumer)

        Events.subscribe(EntityDamageEvent::class.java)
            .filter { it.entityType == EntityType.PLAYER }
            .handler {
                it.isCancelled = true
            }
            .bindWith(consumer)

        Events.subscribe(BlockBreakEvent::class.java)
            .filter { !it.player.hasPermission("server.build") }
            .handler {
                it.isCancelled = true
            }
            .bindWith(consumer)

        Events.subscribe(BlockPlaceEvent::class.java)
            .filter { !it.player.hasPermission("server.build") }
            .handler {
                it.isCancelled = true
            }
            .bindWith(consumer)
    }
}