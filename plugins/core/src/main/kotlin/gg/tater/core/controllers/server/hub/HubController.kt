package gg.tater.core.controllers.server.hub

import gg.tater.shared.annotation.Controller
import gg.tater.shared.network.server.ServerType
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.weather.WeatherChangeEvent

@Controller(
    id = "hub-controller",
    ignoredBinds = [
        ServerType.ONEBLOCK_SERVER,
        ServerType.ONEBLOCK_SPAWN,
        ServerType.ONEBLOCK_PVP,
        ServerType.ONEBLOCK_PLANET
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
    }

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(PlayerJoinEvent::class.java)
            .handler {
                val player = it.player

                it.joinMessage(null)

                Schedulers.sync().runLater({
                    player.teleportAsync(SPAWN_LOCATION)
                    player.health = 20.0
                    player.foodLevel = 20
                    player.gameMode = GameMode.ADVENTURE
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