package gg.tater.oneblock.island.controllers

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.api.loaders.SlimeLoader
import com.infernalsuite.aswm.api.world.SlimeWorld
import com.infernalsuite.aswm.api.world.properties.SlimeProperties
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap
import com.infernalsuite.aswm.loaders.redis.RedisLoader
import gg.tater.oneblock.island.OneBlockIsland
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

    private companion object {
        val PROPERTIES = SlimePropertyMap().apply {
            setValue(SlimeProperties.SPAWN_X, 0)
            setValue(SlimeProperties.SPAWN_Y, 101)
            setValue(SlimeProperties.SPAWN_Z, 0)
            setValue(SlimeProperties.PVP, false)
            setValue(SlimeProperties.ALLOW_ANIMALS, true)
            setValue(SlimeProperties.ALLOW_MONSTERS, true)
        }
    }

    private lateinit var loader: SlimeLoader
    private lateinit var template: SlimeWorld

    override fun setup(consumer: TerminableConsumer) {
        val credential = Services.load(Redis.Credential::class.java)

        registerBaseSubCommands()
        registerMainCommand("island", "is", "ob", "oneblock")

        this.loader = RedisLoader("redis://:${credential.password}@${credential.address}:${credential.port}")
        this.template = AdvancedSlimePaperAPI.instance().readWorld(loader, "island_world_template", false, PROPERTIES)
    }

    override fun loader(): SlimeLoader {
        return loader
    }

    override fun template(): SlimeWorld {
        return template
    }
}