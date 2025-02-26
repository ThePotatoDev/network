package gg.tater.shared.player

import gg.tater.shared.annotation.Controller
import gg.tater.shared.server.ServerDataService
import gg.tater.shared.player.PlayerService.Companion.PLAYER_MAP_NAME
import gg.tater.shared.redis.Redis
import gg.tater.shared.redis.transactional
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.event.EventPriority
import org.bukkit.event.player.*
import org.redisson.api.RFuture
import java.util.*

@Controller(
    id = "player-controller"
)
class BasePlayerController :
    PlayerService {

    private val server = Services.load(ServerDataService::class.java).id()
    private val redis = Services.load(Redis::class.java)

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
                .transactional({ map -> map[data.uuid] = data }, onSuccess, onFailure)
        }
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(PlayerService::class.java, this)

        Events.subscribe(PlayerJoinEvent::class.java, EventPriority.HIGHEST)
            .handler {
                it.joinMessage(null)
            }
            .bindWith(consumer)

        Events.subscribe(PlayerQuitEvent::class.java, EventPriority.HIGHEST)
            .handler {
                it.quitMessage(null)
            }
            .bindWith(consumer)

        Events.subscribe(AsyncPlayerPreLoginEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreDisallowedPreLogin())
            .handler {
                val uuid = it.uniqueId
                val name = it.name

                val player = compute(name, uuid).get()
                player.currentServerId = server
                player.online = true
                player.name = name

                save(player)
            }
            .bindWith(consumer)

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

        Events.subscribe(PlayerCommandPreprocessEvent::class.java, EventPriority.LOWEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                val message = it.message
                if (!message.startsWith("/")) return@handler
                if (!message.contains(":")) return@handler
                if (player.hasPermission("server.coloncommands")) return@handler
                it.isCancelled = true
                player.sendMessage(Component.text("You cannot use colon commands.", NamedTextColor.RED))
            }
            .bindWith(consumer)

        Events.subscribe(PlayerAdvancementDoneEvent::class.java)
            .handler {
                it.message(null)
            }
            .bindWith(consumer)
    }
}