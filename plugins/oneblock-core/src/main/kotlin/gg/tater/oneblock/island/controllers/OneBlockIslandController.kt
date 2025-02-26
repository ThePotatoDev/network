package gg.tater.oneblock.island.controllers

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.api.loaders.SlimeLoader
import com.infernalsuite.aswm.api.world.SlimeWorld
import com.infernalsuite.aswm.loaders.redis.RedisLoader
import gg.tater.oneblock.island.OneBlockIsland
import gg.tater.oneblock.island.subcommand.OneBlockPhasesSubCommand
import gg.tater.shared.annotation.Controller
import gg.tater.shared.island.IslandController
import gg.tater.shared.redis.Redis
import gg.tater.shared.server.model.ServerType
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer

@Controller(
    id = "oneblock-island-controller",
    ignoredBinds = [ServerType.HUB]
)
class OneBlockIslandController : IslandController<OneBlockIsland>() {

    private lateinit var loader: SlimeLoader
    private lateinit var template: SlimeWorld

    override fun setup(consumer: TerminableConsumer) {
        val credential = Services.load(Redis.Credential::class.java)

        registerSubCommand(OneBlockPhasesSubCommand())
        registerBaseSubCommands()

        registerBaseListeners(consumer)
        registerBaseFlags(consumer)
        registerBaseSettings(consumer)
        registerMainCommand("island", "is", "ob", "oneblock")

        this.loader = RedisLoader("redis://:${credential.password}@${credential.address}:${credential.port}")
        this.template = AdvancedSlimePaperAPI.instance().readWorld(loader, "island_world_template", false, properties)
    }

    override fun loader(): SlimeLoader {
        return loader
    }

    override fun template(): SlimeWorld {
        return template
    }
}