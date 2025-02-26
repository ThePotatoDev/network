package gg.tater.shared.scoreboard

import gg.tater.shared.annotation.Controller
import gg.tater.shared.server.ServerDataService
import gg.tater.shared.server.model.toServerType
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary
import org.bukkit.plugin.java.JavaPlugin

@Controller(
    id = "scoreboard-controller"
)
class ScoreboardController(private val plugin: JavaPlugin) : TerminableModule {

    private lateinit var library: ScoreboardLibrary

    private val entries: MutableMap<String, ScoreboardEntry> = mutableMapOf()

    override fun setup(consumer: TerminableConsumer) {
        val serverType = Services.load(ServerDataService::class.java).id().toServerType()

        library = try {
            ScoreboardLibrary.loadScoreboardLibrary(plugin)
        } catch (e: NoPacketAdapterAvailableException) {
            // If no packet adapter was found, you can fall back to the no-op implementation:
            NoopScoreboardLibrary()
        }


    }
}