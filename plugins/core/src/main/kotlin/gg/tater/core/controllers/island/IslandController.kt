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
import gg.tater.shared.UUID_REGEX
import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.flag.IslandFlagController
import gg.tater.shared.island.gui.IslandControlGui
import gg.tater.shared.island.setting.IslandSettingController
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Bukkit
import org.bukkit.World
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class IslandController(
    private val redis: Redis,
    private val server: String,
    private val credential: Redis.Credential
) : IslandService {

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
                return redis.islands()[islandId]!!
            }
        }, SCHEDULER))

    private lateinit var api: AdvancedSlimePaperAPI

    override fun setup(consumer: TerminableConsumer) {
        consumer.bindModule(IslandFlagController(this))
        consumer.bindModule(IslandSettingController(this))

        val flagSubCommand = IslandFlagSubCommand(redis)
        val settingSubCommand = IslandSettingSubCommand(redis)
        commands["create"] = IslandCreateSubCommand(redis)
        commands["home"] = IslandHomeSubCommand(redis, server)
        commands["delete"] = IslandDeleteSubCommand(redis, server)
        commands["visit"] = IslandVisitSubCommand(redis, server)
        commands["invite"] = IslandInviteSubCommand(redis)
        commands["join"] = IslandJoinSubCommand(redis)
        commands["addwarp"] = IslandAddWarpSubCommand(redis)

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

        consumer.bindModule(IslandPlacementRequestListener(redis, server, api, loader, template))
        consumer.bindModule(IslandUpdateRequestListener(redis, server, cache))
        consumer.bindModule(IslandDeleteRequestListener(redis, server, cache))

        Schedulers.async().runRepeating(Runnable {
            for (world in api.loadedWorlds) {
                val worldName = world.name
                val islandId = UUID.fromString(worldName)
                val island = redis.islands()[islandId] ?: continue
                val lastActive = island.lastActivity

                // If 30 seconds of inactivity have not passed, continue to next
                if (Instant.now().isBefore(lastActive.plusSeconds(30L))) continue

                val bukkitWorld = Bukkit.getWorld(worldName) ?: continue
                val empty = bukkitWorld.players.size <= 0

                island.lastActivity = Instant.now()
                redis.islands().fastPut(islandId, island)

                // If the island still has players present on it, keep it loaded
                if (!empty) continue

                Schedulers.sync().run { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "swm unload $worldName") }
                println("Unloading island $worldName due to inactivity.")
            }
        }, 20L, 20L).bindWith(consumer)

        Commands.create()
            .assertPlayer()
            .handler {
                if (it.args().isEmpty()) {
                    val sender = it.sender()
                    redis.players()
                        .getAsync(sender.uniqueId)
                        .thenApplyAsync { data -> data.islandId?.let { id -> redis.islands()[id] } }
                        .thenAccept { island ->
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
}