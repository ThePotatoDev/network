package gg.tater.core.controllers.island

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.api.world.properties.SlimeProperties
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap
import com.infernalsuite.aswm.loaders.redis.RedisLoader
import gg.tater.core.controllers.island.listener.IslandDeleteRequestListener
import gg.tater.core.controllers.island.listener.IslandPlacementRequestListener
import gg.tater.core.controllers.island.listener.IslandUpdateRequestListener
import gg.tater.core.controllers.island.subcommand.*
import gg.tater.shared.annotation.Controller
import gg.tater.shared.UUID_REGEX
import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.IslandService.Companion.ISLAND_INVITES_MAP_NAME
import gg.tater.shared.island.IslandService.Companion.ISLAND_MAP_NAME
import gg.tater.shared.island.flag.IslandFlagController
import gg.tater.shared.island.gui.IslandControlGui
import gg.tater.shared.island.message.placement.IslandPlacementRequest
import gg.tater.shared.island.setting.IslandSettingController
import gg.tater.shared.network.server.ServerDataModel
import gg.tater.shared.network.server.ServerDataService
import gg.tater.shared.network.server.ServerType
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.player.PlayerService
import gg.tater.shared.redis.Redis
import gg.tater.shared.redis.transactional
import me.lucko.helper.Commands
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.promise.ThreadContext
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.redisson.api.RFuture
import org.redisson.api.RMap
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@Controller(
    id = "island-controller"
)
class IslandController : IslandService {

    companion object {
        val SCHEDULER: ScheduledExecutorService = Executors.newScheduledThreadPool(5)

        val PROPERTIES = SlimePropertyMap().apply {
            setValue(SlimeProperties.SPAWN_X, 0)
            setValue(SlimeProperties.SPAWN_Y, 101)
            setValue(SlimeProperties.SPAWN_Z, 0)
            setValue(SlimeProperties.PVP, false)
            setValue(SlimeProperties.ALLOW_ANIMALS, true)
            setValue(SlimeProperties.ALLOW_MONSTERS, true)
        }
    }

    private val redis = Services.load(Redis::class.java)

    private val commands: MutableMap<String, IslandSubCommand> = mutableMapOf()

    /**
     * Store islands temporarily when needed in runtime by world name for usage
     * on main thread without impacting main thread continually w/redis actions
     */
    private val cache = CacheBuilder.newBuilder()
        .refreshAfterWrite(Duration.ofMinutes(1L))
        .build(CacheLoader.asyncReloading(object : CacheLoader<String, Island>() {
            override fun load(worldName: String): Island {
                val islandId = UUID.fromString(worldName)
                return getIsland(islandId).get()!!
            }
        }, SCHEDULER))

    private lateinit var api: AdvancedSlimePaperAPI

    override fun setup(consumer: TerminableConsumer) {
        val credential = Services.load(Redis.Credential::class.java)

        consumer.bindModule(IslandSettingController())
        consumer.bindModule(IslandFlagController())

        val flagSubCommand = IslandFlagSubCommand(redis)
        val settingSubCommand = IslandSettingSubCommand(redis)
        commands["create"] = IslandCreateSubCommand(redis)
        commands["home"] = IslandHomeSubCommand()
        commands["delete"] = IslandDeleteSubCommand(redis)
        commands["visit"] = IslandVisitSubCommand()
        commands["invite"] = IslandInviteSubCommand()
        commands["join"] = IslandJoinSubCommand()
        commands["addwarp"] = IslandAddWarpSubCommand()

        for (setting in listOf("setting", "settings")) {
            commands[setting] = settingSubCommand
        }

        for (flag in listOf("flag", "flags")) {
            commands[flag] = flagSubCommand
        }

        this.api = AdvancedSlimePaperAPI.instance()
        val loader =
            RedisLoader("redis://:${credential.password}@${credential.address}:${credential.port}")
        val template = api.readWorld(loader, "island_world_template", false, PROPERTIES)

        consumer.bindModule(IslandPlacementRequestListener(api, loader, template))
        consumer.bindModule(IslandUpdateRequestListener(cache))
        consumer.bindModule(IslandDeleteRequestListener(cache))

        Schedulers.async().runRepeating(Runnable {
            for (world in api.loadedWorlds) {
                val worldName = world.name
                val islandId = UUID.fromString(worldName)
                val island = getIsland(islandId).get() ?: continue
                val lastActive = island.lastActivity

                // If 30 seconds of inactivity have not passed, continue to next
                if (Instant.now().isBefore(lastActive.plusSeconds(30L))) continue

                val bukkitWorld = Bukkit.getWorld(worldName) ?: continue
                val empty = bukkitWorld.players.size <= 0

                island.lastActivity = Instant.now()
                save(island)

                // If the island still has players present on it, keep it loaded
                if (!empty) continue

                Schedulers.sync().run { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "swm unload $worldName") }
                println("Unloading island $worldName due to inactivity.")
            }
        }, 20L, 20L).bindWith(consumer)

        Commands.create()
            .assertPlayer()
            .handler {
                val players: PlayerService = Services.load(PlayerService::class.java)
                val server = Services.load(ServerDataService::class.java).id()

                if (it.args().isEmpty()) {
                    val sender = it.sender()

                    players.get(sender.uniqueId).thenAcceptAsync { player ->
                        val island = getIslandFor(player)?.get()
                        IslandControlGui(sender, island, redis, server).open()
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
            .registerAndBind(consumer, "is", "island")

        Services.provide(IslandService::class.java, this)
    }

    override fun getIsland(world: World): Island? {
        val name = world.name
        if (!name.matches(UUID_REGEX)) return null
        return cache.get(name)
    }

    override fun all(): RFuture<Collection<Island>> {
        return Services.load(Redis::class.java).client.getMap<UUID, Island>(ISLAND_MAP_NAME)
            .readAllValuesAsync()
    }

    override fun getIslandFor(player: PlayerDataModel): RFuture<Island?>? {
        if (player.islandId == null) return null
        return redis.client.getMap<UUID, Island>(ISLAND_MAP_NAME)
            .getAsync(player.islandId)
    }

    override fun getIsland(islandId: UUID): RFuture<Island?> {
        return redis.client.getMap<UUID, Island>(ISLAND_MAP_NAME)
            .getAsync(islandId)
    }

    override fun save(island: Island): RFuture<Boolean> {
        return redis.client.getMap<UUID, Island>(ISLAND_MAP_NAME)
            .fastPutAsync(island.id, island)
    }

    override fun hasInvite(uuid: UUID, island: Island): RFuture<Boolean> {
        return redis.client.getListMultimapCache<UUID, UUID>(ISLAND_INVITES_MAP_NAME)
            .containsEntryAsync(uuid, uuid)
    }

    override fun addInvite(uuid: UUID, island: Island): RFuture<Boolean> {
        return redis.client.getListMultimapCache<UUID, UUID>(ISLAND_INVITES_MAP_NAME)
            .putAsync(uuid, uuid)
    }

    override fun directToOccupiedServer(sender: Player, island: Island): Boolean {
        if (ThreadContext.forCurrentThread() != ThreadContext.ASYNC) {
            throw IllegalStateException("This method must be called asynchronously.")
        }

        // If the island is already placed on a server, teleport the player to the server
        val currentServerId = island.currentServerId
        var server: ServerDataModel?

        if (currentServerId != null) {
            server = redis.getServer(currentServerId).get()

            // If the server is not online, place the island on a fresh server
            if (server == null) {
                server = redis.getServer(ServerType.SERVER) ?: return false
            }
        } else {
            // If everything else fails to check, place the island on a fresh server
            server = redis.getServer(ServerType.SERVER) ?: return false
        }

        redis.publish(IslandPlacementRequest.of(sender, island, server))
        island.currentServerId = server.id
        save(island)
        return true
    }

    override fun transaction(
        operation: (RMap<UUID, Island>) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        redis.client.apply {
            this.getMap<UUID, Island>(ISLAND_MAP_NAME).transactional(operation, onSuccess, onFailure)
        }
    }
}