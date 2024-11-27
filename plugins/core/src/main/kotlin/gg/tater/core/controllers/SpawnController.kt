package gg.tater.core.controllers

import gg.tater.shared.redis.Redis
import gg.tater.shared.network.model.ServerType
import gg.tater.shared.player.PlayerRedirectRequest
import gg.tater.shared.player.position.PlayerPositionResolver
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.weather.WeatherChangeEvent

class SpawnController(private val redis: Redis, private val id: String) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        Commands.create()
            .assertPlayer()
            .handler {
                val player = it.sender()
                val uuid = player.uniqueId

                redis.players().getAsync(uuid).thenAcceptAsync { data ->
                    if (data == null) {
                        it.reply("&cError fetching your data.")
                        return@thenAcceptAsync
                    }

                    it.reply("&a&oTeleporting you to spawn...")
                    data.setDefaultSpawn(ServerType.SPAWN)
                    data.setPositionResolver(PlayerPositionResolver.Type.TELEPORT_SPAWN)
                    val spawn = ServerType.SPAWN.spawn

                    // If they are already on a spawn server, just teleport them to location
                    if (id.contains("spawn")) {
                        player.teleportAsync(Location(player.world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
                        return@thenAcceptAsync
                    }

                    redis.players().fastPut(uuid, data)
                    val request = PlayerRedirectRequest(uuid, ServerType.SPAWN)
                    redis.publish(request)
                }
            }
            .registerAndBind(consumer, "spawn")

        if (!id.contains("spawn")) {
            return
        }

        Events.subscribe(EntityDamageEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val entity = it.entity
                if (entity.type != EntityType.PLAYER) return@handler
                val world = entity.world

                // We might want to allow damage in other worlds such as pvp
                if (!world.name.equals("world", true)) return@handler

                it.isCancelled = true
            }
            .bindWith(consumer)

        Events.subscribe(WeatherChangeEvent::class.java)
            .handler { it.isCancelled = true }
            .bindWith(consumer)

        Events.subscribe(EntityDamageByEntityEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val entity = it.entity
                if (entity.type != EntityType.PLAYER) return@handler

                val damager = it.damager
                if (damager.type != EntityType.PLAYER) return@handler

                val world = entity.world

                // We might want to allow damage in other worlds such as pvp
                if (!world.name.equals("world", true)) return@handler

                it.isCancelled = true
            }
            .bindWith(consumer)

        Events.subscribe(PlayerArmorStandManipulateEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                it.isCancelled = !player.hasPermission("server.build")
            }
            .bindWith(consumer)

        Events.subscribe(BlockBreakEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                it.isCancelled = !player.hasPermission("server.build")
            }
            .bindWith(consumer)

        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                val action = it.action
                if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
                    it.isCancelled = !player.hasPermission("server.build")
                }
            }
            .bindWith(consumer)

        Events.subscribe(BlockPlaceEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                it.isCancelled = !player.hasPermission("server.build")
            }
            .bindWith(consumer)
    }
}