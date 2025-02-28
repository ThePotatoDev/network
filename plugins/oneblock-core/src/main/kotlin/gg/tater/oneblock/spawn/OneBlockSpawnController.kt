package gg.tater.oneblock.spawn

import gg.tater.oneblock.player.OneBlockPlayerService
import gg.tater.core.annotation.Controller
import gg.tater.core.island.player.position.PositionDirector
import gg.tater.core.player.PlayerRedirectRequest
import gg.tater.core.redis.Redis
import gg.tater.core.server.ServerDataService
import gg.tater.core.server.model.ServerType
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit
import org.bukkit.Difficulty
import org.bukkit.GameRule
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

@Controller(
    id = "oneblock-spawn-controller"
)
class OneBlockSpawnController : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        val redis = Services.load(Redis::class.java)
        val serverType = Services.load(ServerDataService::class.java).serverType()

        // Set basic world settings
        val spawnWorld = Bukkit.getWorld("world")!!
        spawnWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false)
        spawnWorld.difficulty = Difficulty.PEACEFUL

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
                val players: OneBlockPlayerService = Services.load(OneBlockPlayerService::class.java)

                players.get(sender.uniqueId).thenAcceptAsync { player ->
                    if (player == null) {
                        it.reply("&cError fetching your data.")
                        return@thenAcceptAsync
                    }

                    it.reply("&a&oTeleporting you to spawn...")
                    val spawn = ServerType.ONEBLOCK_SPAWN.spawn!!

                    // If they are already on a spawn server, just teleport them to location
                    if (serverType == ServerType.ONEBLOCK_SPAWN) {
                        it.sender().teleportAsync(
                            Location(
                                it.sender().world,
                                spawn.x,
                                spawn.y,
                                spawn.z,
                                spawn.yaw,
                                spawn.pitch
                            )
                        )
                        return@thenAcceptAsync
                    }

                    players.transaction(
                        player.setNextServerSpawnPos(
                            ServerType.ONEBLOCK_SPAWN,
                            PositionDirector.WORLD_TELEPORT_DIRECTOR,
                            spawn
                        ), onSuccess = {
                            redis.publish(PlayerRedirectRequest(player.uuid, ServerType.ONEBLOCK_SPAWN))
                        })
                }
            }
            .registerAndBind(consumer, "spawn")

        // Don't register listeners (not all oneblock related servers need these)
        if (serverType != ServerType.ONEBLOCK_SPAWN) {
            return
        }

        Events.subscribe(EntityDamageEvent::class.java, EventPriority.LOWEST)
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

        Events.subscribe(EntityDamageByEntityEvent::class.java, EventPriority.LOWEST)
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