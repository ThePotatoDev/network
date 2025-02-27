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
import gg.tater.shared.island.player.IslandPlayer
import gg.tater.shared.island.player.IslandPlayerService
import gg.tater.shared.island.setting.IslandSettingController
import gg.tater.shared.server.model.ServerType
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

abstract class IslandController<T : Island, K : IslandPlayer> : TerminableModule {

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
        consumer.bindModule(IslandPlacementRequestListener<T, K>(loader(), template(), properties))
        consumer.bindModule(IslandUpdateRequestListener<T>())
        consumer.bindModule(IslandDeleteRequestListener<T>())
    }

    fun registerBaseSubCommands(serverType: ServerType) {
        registerSubCommand(IslandAddWarpSubCommand<T, K>())
        registerSubCommand(IslandCreateSubCommand<T, K>(serverType))
        registerSubCommand(IslandDeleteSubCommand<T, K>())
        registerSubCommand(IslandFlagSubCommand<T, K>())
        registerSubCommand(IslandHomeSubCommand<T, K>(serverType))
        registerSubCommand(IslandInviteSubCommand<T, K>())
        registerSubCommand(IslandJoinSubCommand<T, K>())
        registerSubCommand(IslandSettingSubCommand<T, K>())
        registerSubCommand(IslandVisitSubCommand<T, K>())
    }

    fun registerMainCommand(vararg aliases: String) {
        Commands.create()
            .assertPlayer()
            .handler {
                val players: IslandPlayerService<K> =
                    Services.load(IslandPlayerService::class.java) as IslandPlayerService<K>
                val islands: IslandService<T, K> = Services.load(IslandService::class.java) as IslandService<T, K>

                if (it.args().isEmpty()) {
                    val sender = it.sender()

                    players.get(sender.uniqueId).thenAcceptAsync { player ->
                        val island = islands.getIslandFor(player)?.get()
                        IslandControlGui<T, K>(sender, island).open()
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