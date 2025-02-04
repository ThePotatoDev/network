package gg.tater.core.controllers.player

import gg.tater.core.CorePlugin
import gg.tater.core.controllers.island.IslandController
import gg.tater.core.controllers.player.auction.AuctionHouseController
import gg.tater.core.controllers.player.chat.PlayerChatController
import gg.tater.core.controllers.player.economy.EconomyController
import gg.tater.core.controllers.player.kit.KitController
import gg.tater.core.controllers.player.playershop.PlayerShopController
import gg.tater.core.controllers.player.pm.PlayerPrivateMessageController
import gg.tater.core.controllers.player.vault.PlayerVaultController
import gg.tater.shared.DECIMAL_FORMAT
import gg.tater.shared.MINI_MESSAGE
import gg.tater.shared.getFormattedDate
import gg.tater.shared.player.economy.EconomyType
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.player.position.resolver.*
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.CollectionSidebarAnimation
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerController(
    private val plugin: CorePlugin,
    private val redis: Redis,
    private val server: String,
    private val islands: IslandController
) :
    TerminableModule {

    private val handlers: MutableMap<PlayerPositionResolver.Type, PlayerPositionResolver> = mutableMapOf()
    private val sidebars: MutableMap<UUID, Pair<Sidebar, ComponentSidebarLayout>> = ConcurrentHashMap(WeakHashMap())

    private lateinit var animation: SidebarAnimation<Component>
    private lateinit var scoreboardLibrary: ScoreboardLibrary

    override fun setup(consumer: TerminableConsumer) {
        scoreboardLibrary = try {
            ScoreboardLibrary.loadScoreboardLibrary(plugin)
        } catch (e: NoPacketAdapterAvailableException) {
            // If no packet adapter was found, you can fall back to the no-op implementation:
            NoopScoreboardLibrary()
        }

        consumer.bindModule(PlayerPrivateMessageController(redis))
        consumer.bindModule(PlayerChatController(redis))
        consumer.bindModule(KitController(redis))
        consumer.bindModule(AuctionHouseController(redis))
        consumer.bindModule(EconomyController(redis))
        consumer.bindModule(PlayerVaultController(redis))
        consumer.bindModule(PlayerShopController(redis, islands, server))

        handlers[PlayerPositionResolver.Type.TELEPORT_SPAWN] = SpawnPositionResolver(redis)
        handlers[PlayerPositionResolver.Type.TELEPORT_ISLAND_HOME] = IslandHomePositionResolver(redis)
        handlers[PlayerPositionResolver.Type.TELEPORT_ISLAND_VISIT] = IslandVisitPositionResolver(redis)
        handlers[PlayerPositionResolver.Type.TELEPORT_ISLAND_WARP] = IslandWarpPositionResolver(redis)
        handlers[PlayerPositionResolver.Type.TELEPORT_PLAYER_SHOP] = PlayerShopPositionResolver(redis)

        Commands.create()
            .assertPlayer()
            .assertPermission("server.heal")
            .handler {
                val sender = it.sender()
                sender.health = 20.0
                sender.foodLevel = 20
                it.reply("&aYou have been healed")
            }
            .registerAndBind(consumer, "heal")

        Commands.create()
            .assertPlayer()
            .assertPermission("server.gamemode")
            .handler {
                val sender = it.sender()
                val label = it.label()

                if (label.equals("gmc", true)) {
                    sender.gameMode = GameMode.CREATIVE
                    it.reply("&aGamemode is now creative.")
                    return@handler
                }

                if (label.equals("gms", true)) {
                    sender.gameMode = GameMode.SURVIVAL
                    it.reply("&aGamemode is now survival.")
                }
            }
            .registerAndBind(consumer, "gmc", "gms")

        Events.subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.joinMessage(null)
                val currentServer = redis.servers()[server] ?: return@handler

                display(it.player)

                redis.players().getAsync(it.player.uniqueId).thenAcceptAsync { player ->
                    val resolver = player.resolver!!
                    val handler = handlers[resolver.first] ?: return@thenAcceptAsync
                    val location = handler.getLocation(player, currentServer.type).join() ?: return@thenAcceptAsync

                    Schedulers.sync().runLater({
                        it.player.teleportAsync(location)
                        player.apply(it.player)
                    }, 2L)

                    player.online = true
                    redis.players().fastPut(player.uuid, player.setPositionResolver(PlayerPositionResolver.Type.NONE))
                }
            }
            .bindWith(consumer)

        Events.subscribe(PlayerQuitEvent::class.java)
            .handler {
                it.quitMessage(null)
                val uuid = it.player.uniqueId
                sidebars.remove(uuid)

                redis.players().getAsync(uuid).thenAcceptAsync { player ->
                    val server = redis.servers()[server] ?: return@thenAcceptAsync
                    redis.players().fastPut(uuid, player.update(it.player, server.type))
                }
            }
            .bindWith(consumer)

        Events.subscribe(PlayerAdvancementDoneEvent::class.java)
            .handler {
                it.message(null)
            }
            .bindWith(consumer)

        this.animation =
            createGradientAnimation(Component.text("ꜱᴋʏʟᴀɴᴅꜱ", Style.style(TextDecoration.BOLD)))

        Schedulers.async().runRepeating(Runnable {
            for (data in sidebars.values) {
                animation.nextFrame()
                data.second.apply(data.first)
            }
        }, 5L, 5L).bindWith(consumer)
    }

    private fun display(player: Player) {
        val sidebar: Sidebar = scoreboardLibrary.createSidebar()

        val title = SidebarComponent.animatedLine(animation)
        val lines = SidebarComponent.builder()
            .addStaticLine(Component.text(getFormattedDate(), NamedTextColor.GRAY))
            .addBlankLine()
            .addStaticLine(
                Component.text("• ʏᴏᴜ: ", NamedTextColor.GRAY)
                    .append(Component.text(player.name, NamedTextColor.WHITE))
            )
            .addDynamicLine {
                val balance = redis.economy()[player.uniqueId]?.get(EconomyType.MONEY) ?: 0
                Component.text("• ʙᴀʟᴀɴᴄᴇ: ", NamedTextColor.GRAY)
                    .append(Component.text("$${DECIMAL_FORMAT.format(balance)}", NamedTextColor.WHITE))
            }
            .addDynamicLine {
                Component.text("• ᴏɴʟɪɴᴇ: ", NamedTextColor.GRAY)
                    .append(Component.text(0, NamedTextColor.WHITE))
            }
            .addBlankLine()
            .addStaticLine(Component.text("ᴡᴡᴡ.ꜱᴋʏʟᴀɴᴅꜱ.ᴄᴏᴍ", NamedTextColor.GOLD))
            .build()

        val layout = ComponentSidebarLayout(title, lines)
        layout.apply(sidebar)
        sidebar.addPlayer(player)

        sidebars[player.uniqueId] = Pair(sidebar, layout)
    }

    private fun createGradientAnimation(text: Component): SidebarAnimation<Component> {
        val step = 1f / 8f

        val textPlaceholder = Placeholder.component("text", text)
        val frames: MutableList<Component> = ArrayList((2f / step).toInt())

        var phase = -1f
        while (phase < 1) {
            frames.add(MINI_MESSAGE.deserialize("<gradient:yellow:gold:$phase><text>", textPlaceholder))
            phase += step
        }

        return CollectionSidebarAnimation(frames)
    }
}