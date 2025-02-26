package gg.tater.shared.island

import com.infernalsuite.aswm.api.loaders.SlimeLoader
import com.infernalsuite.aswm.api.world.SlimeWorld
import gg.tater.shared.island.command.IslandSubCommand
import gg.tater.shared.island.command.base.*
import gg.tater.shared.island.gui.IslandControlGui
import gg.tater.shared.player.PlayerService
import gg.tater.shared.server.ServerDataService
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.module.TerminableModule

abstract class IslandController<T : Island> : TerminableModule {

    private val commands: MutableMap<String, IslandSubCommand<T>> = mutableMapOf()

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