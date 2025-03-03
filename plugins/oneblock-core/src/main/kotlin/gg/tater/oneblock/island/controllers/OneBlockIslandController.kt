package gg.tater.oneblock.island.controllers

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.api.loaders.SlimeLoader
import com.infernalsuite.aswm.api.world.SlimeWorld
import com.infernalsuite.aswm.loaders.redis.RedisLoader
import gg.tater.core.annotation.Controller
import gg.tater.core.island.IslandController
import gg.tater.core.island.cache.IslandWorldCacheService
import gg.tater.core.island.flag.model.FlagType
import gg.tater.core.redis.Redis
import gg.tater.core.server.model.ServerType
import gg.tater.oneblock.event.OneBlockMineEvent
import gg.tater.oneblock.island.OneBlockIsland
import gg.tater.oneblock.island.subcommand.OneBlockPhasesSubCommand
import gg.tater.oneblock.player.OneBlockPlayer
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent

@Controller(
    id = "oneblock-island-controller"
)
class OneBlockIslandController : IslandController<OneBlockIsland, OneBlockPlayer>() {

    private val cache = Services.load(IslandWorldCacheService::class.java)

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

        Events.subscribe(BlockBreakEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                val block = it.block
                val island = cache.getIsland(block.world) ?: return@handler

                if (!island.canInteract(player.uniqueId, FlagType.BREAK_BLOCKS)) return@handler

                val location = block.location

                // If the block location matches the specified breakable oneblock,
                // fire the OneBlockMineEvent.
                if (OneBlockIsland.ONE_BLOCK_LOCATION.x == location.x
                    && OneBlockIsland.ONE_BLOCK_LOCATION.y == location.y
                    && OneBlockIsland.ONE_BLOCK_LOCATION.z == location.z
                ) {
                    Events.callSync(OneBlockMineEvent(player, island as OneBlockIsland))
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