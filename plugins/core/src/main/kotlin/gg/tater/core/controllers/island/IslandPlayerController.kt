package gg.tater.core.controllers.island

import gg.tater.shared.annotation.Controller
import gg.tater.shared.network.server.ServerDataService
import gg.tater.shared.network.server.ServerType
import gg.tater.shared.network.server.toServerType
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.player.position.resolver.*
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@Controller(
    id = "island-player-controller",
    ignoredBinds = [ServerType.HUB]
)
class IslandPlayerController : TerminableModule {

    private val server = Services.load(ServerDataService::class.java).id()
    private val handlers: MutableMap<PlayerPositionResolver.Type, PlayerPositionResolver> = mutableMapOf()

    override fun setup(consumer: TerminableConsumer) {
        handlers[PlayerPositionResolver.Type.TELEPORT_PLAYER_SHOP] = PlayerShopPositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_SPAWN] = SpawnPositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_ISLAND_HOME] = IslandHomePositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_ISLAND_WARP] = IslandWarpPositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_ISLAND_VISIT] = IslandVisitPositionResolver()
        handlers[PlayerPositionResolver.Type.TELEPORT_SERVER_WARP] = ServerWarpPositionResolver()

        Events.subscribe(PlayerJoinEvent::class.java)
            .handler {
                val players = Services.load(PlayerService::class.java)

                players.get(it.player.uniqueId).thenAcceptAsync { player ->
                    val resolver = player.resolver!!
                    val handler = handlers[resolver.first] ?: return@thenAcceptAsync
                    val location = handler.getLocation(player, server.toServerType()).join() ?: return@thenAcceptAsync

                    Schedulers.sync().runLater({
                        it.player.teleportAsync(location)
                        player.apply(it.player)
                    }, 2L)
                }
            }
            .bindWith(consumer)

        Events.subscribe(PlayerQuitEvent::class.java)
            .handler {
                val players = Services.load(PlayerService::class.java)
                val uuid = it.player.uniqueId
                players.get(uuid).thenAcceptAsync { player ->
                    players.save(player.update(it.player, server.toServerType()))
                }
            }
            .bindWith(consumer)
    }
}