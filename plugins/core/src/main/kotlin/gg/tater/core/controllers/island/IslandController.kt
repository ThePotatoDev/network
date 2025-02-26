package gg.tater.core.controllers.island

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.api.world.properties.SlimeProperties
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap
import com.infernalsuite.aswm.loaders.redis.RedisLoader
import gg.tater.core.controllers.island.subcommand.*
import gg.tater.shared.annotation.Controller
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.flag.IslandFlagController
import gg.tater.shared.island.gui.IslandControlGui
import gg.tater.shared.island.message.listener.IslandDeleteRequestListener
import gg.tater.shared.island.message.listener.IslandPlacementRequestListener
import gg.tater.shared.island.message.listener.IslandUpdateRequestListener
import gg.tater.shared.island.message.placement.IslandPlacementRequest
import gg.tater.shared.island.setting.IslandSettingController
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.redis.Redis
import gg.tater.shared.redis.transactional
import gg.tater.shared.server.ServerDataService
import gg.tater.shared.server.model.GameModeType
import gg.tater.shared.server.model.ServerDataModel
import gg.tater.shared.server.model.ServerType
import gg.tater.shared.server.model.toServerType
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import org.redisson.api.RMap
import java.util.*

@Controller(
    id = "island-controller"
)
class IslandController : TerminableModule {

    private val redis = Services.load(Redis::class.java)
    private val serverType = Services.load(ServerDataService::class.java)
        .id()
        .toServerType()

    private val commands: MutableMap<String, IslandSubCommand> = mutableMapOf()

    private lateinit var api: AdvancedSlimePaperAPI

    override fun setup(consumer: TerminableConsumer) {
        // If server type is hub, just provide the service
        if (serverType == ServerType.HUB) return

        val credential = Services.load(Redis.Credential::class.java)

        consumer.bindModule(IslandSettingController())
        consumer.bindModule(IslandFlagController())

        val flagSubCommand = IslandFlagSubCommand(redis)
        val settingSubCommand = IslandSettingSubCommand(redis)
        commands["create"] = IslandCreateSubCommand(redis)
        commands["home"] = IslandHomeSubCommand()
        commands["delete"] = IslandDeleteSubCommand(redis)
        commands["visit"] = IslandVisitSubCommand()
        commands["invite"] = IslandInviteSubCommand()
        commands["join"] = IslandJoinSubCommand()
        commands["addwarp"] = IslandAddWarpSubCommand()

        for (setting in listOf("setting", "settings")) {
            commands[setting] = settingSubCommand
        }

        for (flag in listOf("flag", "flags")) {
            commands[flag] = flagSubCommand
        }

        this.api = AdvancedSlimePaperAPI.instance()
        val loader =
            RedisLoader("redis://:${credential.password}@${credential.address}:${credential.port}")
        val template = api.readWorld(loader, "island_world_template", false, PROPERTIES)

        consumer.bindModule(IslandPlacementRequestListener(api, loader, template, PROPERTIES))
        consumer.bindModule(IslandUpdateRequestListener())
        consumer.bindModule(IslandDeleteRequestListener())

        Commands.create()
            .assertPlayer()
            .handler {
                val players: PlayerService = Services.load(PlayerService::class.java)
                val server = Services.load(ServerDataService::class.java).id()

                if (it.args().isEmpty()) {
                    val sender = it.sender()

                    players.get(sender.uniqueId).thenAcceptAsync { player ->
                        val island = getIslandFor(player)?.get()
                        IslandControlGui(sender, island, redis, server).open()
                    }
                    return@handler
                }

                val arg = it.arg(0).parseOrFail(String::class.java)
                val command = commands[arg.lowercase()]

                if (command == null) {
                    it.reply("&cUnknown subcommand.")
                    return@handler
                }

                command.handle(it)
            }
            .registerAndBind(consumer, "is", "island")
    }
}