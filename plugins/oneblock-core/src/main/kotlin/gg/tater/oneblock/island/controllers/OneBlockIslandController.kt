package gg.tater.oneblock.island.controllers

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.api.loaders.SlimeLoader
import com.infernalsuite.aswm.api.world.SlimeWorld
import com.infernalsuite.aswm.loaders.redis.RedisLoader
import gg.tater.core.annotation.Controller
import gg.tater.core.island.IslandController
import gg.tater.core.island.cache.IslandWorldCacheService
import gg.tater.core.island.event.IslandPlacementEvent
import gg.tater.core.island.event.IslandUnloadEvent
import gg.tater.core.island.experience.ExperienceService
import gg.tater.core.island.flag.model.FlagType
import gg.tater.core.player.event.PlayerPlacementEvent
import gg.tater.core.redis.Redis
import gg.tater.core.server.model.ServerType
import gg.tater.oneblock.event.OneBlockMineEvent
import gg.tater.oneblock.island.OneBlockIsland
import gg.tater.oneblock.island.phase.subcommand.OneBlockPhasesSubCommand
import gg.tater.oneblock.player.OneBlockPlayer
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack

@Controller(
    id = "oneblock-island-controller"
)
class OneBlockIslandController : IslandController<OneBlockIsland, OneBlockPlayer>() {
    private val cache = Services.load(IslandWorldCacheService::class.java)
    private val experience = Services.load(ExperienceService::class.java)
    private val islands = Services.load(OneBlockIslandService::class.java)

    private lateinit var loader: SlimeLoader
    private lateinit var template: SlimeWorld

    override fun setup(consumer: TerminableConsumer) {
        val credential = Services.load(Redis.Credential::class.java)

        this.loader = RedisLoader("redis://:${credential.password}@${credential.address}:${credential.port}")
        this.template = AdvancedSlimePaperAPI.instance().readWorld(loader, "island_world_template", false, properties)

        registerSubCommand(OneBlockPhasesSubCommand())
        registerBaseSubCommands(ServerType.ONEBLOCK_SERVER)

        registerBaseListeners(consumer)
        registerBaseFlags(consumer)
        registerBaseSettings(consumer)
        registerMainCommand("island", "is", "ob", "oneblock")

        Events.subscribe(PlayerPlacementEvent::class.java)
            .handler {
                islands.getIsland(it.islandId).thenAccept { island ->
                    if (island == null) return@thenAccept
                    if (!island.ftue) return@thenAccept
                    val player = Bukkit.getPlayer(it.player.uuid) ?: return@thenAccept
                    experience.startExperience(player)
                }
            }
            .bindWith(consumer)

        Events.subscribe(IslandPlacementEvent::class.java)
            .handler {
                val island = it.island as OneBlockIsland

                // Spawn with a delay
                Schedulers.sync().runLater({
                    island.spawnShipNPC(it.world)
                    island.spawnBrokenShip(it.world)
                }, 5L)
            }
            .bindWith(consumer)

        Events.subscribe(IslandUnloadEvent::class.java)
            .handler {
                val island = it.island as OneBlockIsland
                island.deleteShipNPC()
                island.deleteBrokenShip()
            }
            .bindWith(consumer)

        Events.subscribe(BlockBreakEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                val block = it.block
                val world = block.world
                val island = cache.getIsland(world) ?: return@handler

                if (!island.canInteract(player.uniqueId, FlagType.BREAK_BLOCKS)) return@handler

                val location = block.location

                // If the block location matches the specified breakable oneblock,
                // fire the OneBlockMineEvent.
                if (OneBlockIsland.ONE_BLOCK_LOCATION.x == location.x
                    && OneBlockIsland.ONE_BLOCK_LOCATION.y == location.y
                    && OneBlockIsland.ONE_BLOCK_LOCATION.z == location.z
                ) {
                    val event = Events.callAndReturn(OneBlockMineEvent(player, island as OneBlockIsland, block))

                    if (event.isCancelled) {
                        it.isCancelled = true
                        return@handler
                    }

                    val oldDrops = block.drops
                    block.drops.clear()

                    val drops: MutableList<ItemStack> = mutableListOf()
                    if (event.dropOldDrops) {
                        drops.addAll(oldDrops)
                    }
                    drops.addAll(event.extraDrops)

                    Schedulers.sync().runLater({
                        // Loop through the drops and spawn them at a better location
                        for (drop in drops) {
                            world.dropItem(
                                block.getRelative(BlockFace.UP)
                                    .location
                                    .add(0.5, 0.0, 0.5), drop
                            )
                        }

                        // If there is a block we are manually setting, handle it instead of their island phase blocks
                        if (event.nextMaterialType != null) {
                            block.type = event.nextMaterialType!!
                        }
                    }, 1L)
                }
            }
            .bindWith(consumer)
    }

    override fun loader(): SlimeLoader {
        return loader
    }

    override fun template(): SlimeWorld {
        return template
    }
}