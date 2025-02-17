package gg.tater.core.controllers.player

import gg.tater.core.CorePlugin
import gg.tater.core.controllers.player.playershop.PlayerShopController
import gg.tater.shared.Controller
import gg.tater.shared.DECIMAL_FORMAT
import gg.tater.shared.MINI_MESSAGE
import gg.tater.shared.getFormattedDate
import gg.tater.shared.network.server.ServerDataService
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.PlayerService.Companion.PLAYER_MAP_NAME
import gg.tater.shared.player.economy.EconomyType
import gg.tater.shared.player.economy.PlayerEconomyService
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.player.position.resolver.*
import gg.tater.shared.redis.Redis
import gg.tater.shared.redis.transactional
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
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
import org.redisson.api.RFuture
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Controller(id = "player-controller")
class PlayerController :
    PlayerService {

    private val server = Services.load(ServerDataService::class.java).id()
    private val plugin = Services.load(CorePlugin::class.java)
    private val redis = Services.load(Redis::class.java)

    private val handlers: MutableMap<PlayerPositionResolver.Type, PlayerPositionResolver> = mutableMapOf()
    private val sidebars: MutableMap<UUID, Pair<Sidebar, ComponentSidebarLayout>> = ConcurrentHashMap(WeakHashMap())

    private lateinit var animation: SidebarAnimation<Component>
    private lateinit var scoreboardLibrary: ScoreboardLibrary

    override fun compute(name: String, uuid: UUID): RFuture<PlayerDataModel> {
        return redis.client.getMap<UUID, PlayerDataModel>(PLAYER_MAP_NAME)
            .computeIfAbsentAsync(uuid) {
                PlayerDataModel(uuid, name)
            }
    }

    override fun get(uuid: UUID): RFuture<PlayerDataModel> {
        return redis.client.getMap<UUID, PlayerDataModel>(PLAYER_MAP_NAME)
            .getAsync(uuid)
    }

    override fun save(data: PlayerDataModel): RFuture<Boolean> {
        return redis.client.getMap<UUID, PlayerDataModel>(PLAYER_MAP_NAME)
            .fastPutAsync(data.uuid, data)
    }

    override fun transaction(data: PlayerDataModel, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        redis.client.apply {
            this.getMap<UUID, PlayerDataModel>(PLAYER_MAP_NAME)
                .transactional(this, { map -> map[data.uuid] = data }, onSuccess, onFailure)
        }
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(PlayerService::class.java, this)

        scoreboardLibrary = try {
            ScoreboardLibrary.loadScoreboardLibrary(plugin)
        } catch (e: NoPacketAdapterAvailableException) {
            // If no packet adapter was found, you can fall back to the no-op implementation:
            NoopScoreboardLibrary()
        }

        consumer.bindModule(PlayerShopController())

        handlers[PlayerPositionResolver.Type.TELEPORT_PLAYER_SHOP] = PlayerShopPositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_SPAWN] = SpawnPositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_ISLAND_HOME] = IslandHomePositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_ISLAND_WARP] = IslandWarpPositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_ISLAND_VISIT] = IslandVisitPositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_SERVER_WARP] = ServerWarpPositionResolver()

        Commands.create()
            .assertPlayer()
            .assertPermission("server.createdata")
            .handler {
                val sender = it.sender()

                compute(sender.name, sender.uniqueId)
                it.reply("Computed data")
            }
            .registerAndBind(consumer, "createdata")

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

                get(it.player.uniqueId).thenAcceptAsync { player ->
                    display(it.player)

                    val resolver = player.resolver!!
                    val handler = handlers[resolver.first] ?: return@thenAcceptAsync
                    val location = handler.getLocation(player, currentServer.type).join() ?: return@thenAcceptAsync

                    Schedulers.sync().runLater({
                        it.player.teleportAsync(location)
                        player.apply(it.player)
                    }, 2L)

                    player.currentServerId = server
                    player.online = true

                    save(player)
                }
            }
            .bindWith(consumer)

        Events.subscribe(PlayerQuitEvent::class.java)
            .handler {
                it.quitMessage(null)
                val uuid = it.player.uniqueId
                sidebars.remove(uuid)

                get(uuid).thenAcceptAsync { player ->
                    val server = redis.servers()[server] ?: return@thenAcceptAsync
                    save(player.update(it.player, server.type))
                }
            }
            .bindWith(consumer)

        Events.subscribe(PlayerAdvancementDoneEvent::class.java)
            .handler {
                it.message(null)
            }
            .bindWith(consumer)

        this.animation =
            createGradientAnimation(Component.text("ᴏɴᴇʙʟᴏᴄᴋ", Style.style(TextDecoration.BOLD)))

        Schedulers.async().runRepeating(Runnable {
            for (data in sidebars.values) {
                animation.nextFrame()
                data.second.apply(data.first)
            }
        }, 5L, 5L).bindWith(consumer)
    }

    private fun display(player: Player) {
        val eco = Services.load(PlayerEconomyService::class.java)

        val sidebar: Sidebar = scoreboardLibrary.createSidebar()
        val data = redis.proxy().get()

        val title = SidebarComponent.animatedLine(animation)
        val lines = SidebarComponent.builder()
            .addStaticLine(Component.text(getFormattedDate(), NamedTextColor.GRAY))
            .addBlankLine()
            .addStaticLine(
                Component.text("• ʏᴏᴜ: ", NamedTextColor.GRAY)
                    .append(Component.text(player.name, NamedTextColor.WHITE))
            )
            .addDynamicLine {
                val balance = eco.getSync(player.uniqueId)?.get(EconomyType.MONEY) ?: 0
                Component.text("• ʙᴀʟᴀɴᴄᴇ: ", NamedTextColor.GRAY)
                    .append(Component.text("$${DECIMAL_FORMAT.format(balance)}", NamedTextColor.WHITE))
            }
            .addDynamicLine {
                Component.text("• ᴏɴʟɪɴᴇ: ", NamedTextColor.GRAY)
                    .append(Component.text(data.players, NamedTextColor.WHITE))
            }
            .addBlankLine()
            .addStaticLine(MINI_MESSAGE.deserialize("<gradient:#8A15B1:#AE07B7>ᴡᴡᴡ.ᴏɴᴇʙʟᴏᴄᴋ.ɪꜱ"))
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
            frames.add(MINI_MESSAGE.deserialize("<gradient:#8A15B1:#AE07B7:$phase><text>", textPlaceholder))
            phase += step
        }

        return CollectionSidebarAnimation(frames)
    }
}