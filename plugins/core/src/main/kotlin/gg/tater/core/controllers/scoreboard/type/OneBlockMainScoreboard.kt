package gg.tater.core.controllers.scoreboard.type

import gg.tater.shared.DECIMAL_FORMAT
import gg.tater.shared.MINI_MESSAGE
import gg.tater.shared.getFormattedDate
import gg.tater.shared.island.IslandService
import gg.tater.shared.network.server.ONEBLOCK_GAMEMODE_SERVERS
import gg.tater.shared.network.server.ServerType
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.economy.EconomyType
import gg.tater.shared.player.economy.PlayerEconomyService
import gg.tater.shared.scoreboard.ScoreboardEntry
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent

class OneBlockMainScoreboard(private val library: ScoreboardLibrary) : ScoreboardEntry {

    override fun id(): String {
        return "oneblock-main"
    }

    override fun display(player: Player) {
        val eco = Services.load(PlayerEconomyService::class.java)
        val islands = Services.load(IslandService::class.java)
        val players = Services.load(PlayerService::class.java)

        val sidebar: Sidebar = library.createSidebar()

        val title = SidebarComponent.staticLine(Component.text("ꑑ"))
        val lines = SidebarComponent.builder()
            .addStaticLine(Component.text(getFormattedDate(), NamedTextColor.GRAY))
            .addBlankLine()
            .addDynamicLine {
                val island = players.get(player.uniqueId)
                    .get()
                    .let { islands.getIslandFor(it)?.get() }

                val level = island?.level ?: 0

                Component.text("ꐡ ")
                    .append(MINI_MESSAGE.deserialize("<gradient:#ECEAB0:#E0C909>Level: "))
                    .append(Component.text("$level", NamedTextColor.WHITE))
            }
            .addDynamicLine {
                Component.text("ꐢ ")
                    .append(MINI_MESSAGE.deserialize("<gradient:#ECEAB0:#E0C909>Progress: "))
                    .append(Component.text("None", NamedTextColor.WHITE))
            }
            .addBlankLine()
            .addDynamicLine {
                val balance = eco.getSync(player.uniqueId)?.get(EconomyType.MONEY) ?: 0
                Component.text("ꐛ ")
                    .append(MINI_MESSAGE.deserialize("<gradient:#ECEAB0:#E0C909>Coins: "))
                    .append(Component.text(DECIMAL_FORMAT.format(balance), NamedTextColor.WHITE))
            }
            .addBlankLine()
            .addStaticLine(MINI_MESSAGE.deserialize("<gradient:#ECEAB0:#E0C909>ᴘʟᴀʏ.ᴏɴᴇʙʟᴏᴄᴋ.ɪꜱ"))
            .build()

        val layout = ComponentSidebarLayout(title, lines)
        layout.apply(sidebar)
        sidebar.addPlayer(player)
    }

    override fun applicableTo(): Set<ServerType> {
        return ONEBLOCK_GAMEMODE_SERVERS
    }

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(PlayerJoinEvent::class.java)
            .handler {
                val player = it.player
                display(player)
            }
            .bindWith(consumer)
    }
}