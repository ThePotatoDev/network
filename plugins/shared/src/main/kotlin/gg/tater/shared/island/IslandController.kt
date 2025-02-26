package gg.tater.shared.island

import com.infernalsuite.aswm.api.loaders.SlimeLoader
import com.infernalsuite.aswm.api.world.SlimeWorld
import com.infernalsuite.aswm.api.world.properties.SlimeProperties
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap
import gg.tater.shared.island.command.IslandSubCommand
import gg.tater.shared.island.command.base.*
import gg.tater.shared.island.flag.IslandFlagController
import gg.tater.shared.island.gui.IslandControlGui
import gg.tater.shared.island.message.listener.IslandDeleteRequestListener
import gg.tater.shared.island.message.listener.IslandPlacementRequestListener
import gg.tater.shared.island.message.listener.IslandUpdateRequestListener
import gg.tater.shared.island.setting.IslandSettingController
import gg.tater.shared.server.ServerDataService
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

abstract class IslandController<T : Island> : TerminableModule {

    val properties = SlimePropertyMap().apply {
        setValue(SlimeProperties.SPAWN_X, 0)
        setValue(SlimeProperties.SPAWN_Y, 101)
        setValue(SlimeProperties.SPAWN_Z, 0)
        setValue(SlimeProperties.PVP, false)
        setValue(SlimeProperties.ALLOW_ANIMALS, true)
        setValue(SlimeProperties.ALLOW_MONSTERS, true)
    }

    private val commands: MutableMap<String, IslandSubCommand<T>> = mutableMapOf()

    fun registerBaseSettings(consumer: TerminableConsumer) {
        consumer.bindModule(IslandSettingController())
    }

    fun registerBaseFlags(consumer: TerminableConsumer) {
        consumer.bindModule(IslandFlagController())
    }

    fun registerBaseListeners(consumer: TerminableConsumer) {
        consumer.bindModule(IslandPlacementRequestListener<T>(loader(), template(), properties))
        consumer.bindModule(IslandUpdateRequestListener<T>())
        consumer.bindModule(IslandDeleteRequestListener<T>())
    }

    fun registerBaseSubCommands() {
        registerSubCommand(IslandAddWarpSubCommand())
        registerSubCommand(IslandCreateSubCommand())
        registerSubCommand(IslandDeleteSubCommand())
        registerSubCommand(IslandFlagSubCommand())
        registerSubCommand(IslandHomeSubCommand())
        registerSubCommand(IslandInviteSubCommand())
        registerSubCommand(IslandJoinSubCommand())
        registerSubCommand(IslandSettingSubCommand())
        registerSubCommand(IslandVisitSubCommand())
    }

    fun registerMainCommand(vararg aliases: String) {
        Commands.create()
            .assertPlayer()
            .handler {
                val players: PlayerService = Services.load(PlayerService::class.java)
                val server = Services.load(ServerDataService::class.java).id()
                val islands: IslandService<T> = Services.load(IslandService::class.java) as IslandService<T>

                if (it.args().isEmpty()) {
                    val sender = it.sender()

                    players.get(sender.uniqueId).thenAcceptAsync { player ->
                        val island = islands.getIslandFor(player)?.get()
                        IslandControlGui(sender, island, server).open()
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
            .register(*aliases)
    }

    fun registerSubCommand(command: IslandSubCommand<T>) {
        commands[command.id().lowercase()] = command
    }

    fun getSubCommand(id: String): IslandSubCommand<T>? {
        return commands[id.lowercase()]
    }

    abstract fun loader(): SlimeLoader

    abstract fun template(): SlimeWorld
}