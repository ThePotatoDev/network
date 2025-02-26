package gg.tater.core.controllers.scoreboard

import gg.tater.core.CorePlugin
import gg.tater.core.controllers.scoreboard.type.OneBlockMainScoreboard
import gg.tater.shared.annotation.Controller
import gg.tater.shared.network.server.ServerDataService
import gg.tater.shared.network.server.toServerType
import gg.tater.shared.scoreboard.ScoreboardEntry
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary

@Controller(
    id = "scoreboard-controller"
)
class ScoreboardController : TerminableModule {

    private lateinit var library: ScoreboardLibrary

    private val entries: MutableMap<String, ScoreboardEntry> = mutableMapOf()

    override fun setup(consumer: TerminableConsumer) {
        val serverType = Services.load(ServerDataService::class.java).id().toServerType()

        library = try {
            ScoreboardLibrary.loadScoreboardLibrary(Services.load(CorePlugin::class.java))
        } catch (e: NoPacketAdapterAvailableException) {
            // If no packet adapter was found, you can fall back to the no-op implementation:
            NoopScoreboardLibrary()
        }

        val oneBlockMain = OneBlockMainScoreboard(library)
        entries[oneBlockMain.id()] = oneBlockMain

        for (scoreboard in entries.values) {
            if (!scoreboard.applicableTo().contains(serverType)) continue
            consumer.bindModule(scoreboard)
        }
    }
}