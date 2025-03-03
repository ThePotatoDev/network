package gg.tater.oneblock.scoreboard

import gg.tater.core.DECIMAL_FORMAT
import gg.tater.core.MINI_MESSAGE
import gg.tater.core.getFormattedDate
import gg.tater.core.island.IslandService
import gg.tater.core.player.economy.model.EconomyType
import gg.tater.core.player.economy.model.PlayerEconomyService
import gg.tater.oneblock.island.OneBlockIsland
import gg.tater.oneblock.island.controllers.OneBlockIslandService
import gg.tater.oneblock.player.OneBlockPlayer
import gg.tater.oneblock.player.OneBlockPlayerService
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class OneBlockMainScoreboard(private val plugin: JavaPlugin) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        val eco = Services.load(PlayerEconomyService::class.java)
        val islands: IslandService<OneBlockIsland, OneBlockPlayer> = Services.load(OneBlockIslandService::class.java)
        val players = Services.load(OneBlockPlayerService::class.java)

        val library = try {
            ScoreboardLibrary.loadScoreboardLibrary(plugin)
        } catch (e: NoPacketAdapterAvailableException) {
            // If no packet adapter was found, you can fall back to the no-op implementation:
            NoopScoreboardLibrary()
        }

        Events.subscribe(PlayerJoinEvent::class.java)
            .handler {
                val player = it.player
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
            .bindWith(consumer)
    }
}