package gg.tater.core

import gg.tater.shared.annotation.Controller
import gg.tater.shared.findAnnotatedClasses
import gg.tater.shared.network.Agones
import gg.tater.shared.network.server.ServerDataService
import gg.tater.shared.redis.Redis
import io.github.cdimascio.dotenv.Dotenv
import me.lucko.helper.Helper
import me.lucko.helper.Services
import me.lucko.helper.plugin.ExtendedJavaPlugin
import me.lucko.helper.terminable.module.TerminableModule
import okhttp3.OkHttpClient
import org.bukkit.Bukkit
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class CorePlugin : ExtendedJavaPlugin(), ServerDataService {

    private lateinit var serverId: String

    override fun enable() {
        Services.provide(CorePlugin::class.java, this)
        val client = Services.provide(OkHttpClient::class.java, OkHttpClient())
        val actions = Services.provide(Agones::class.java, Agones(client))

        val server = actions.getGameServerId()
        if (server == null) {
            logger.severe("Failed to get game server id")
            Bukkit.shutdown()
            return
        }

        this.serverId = server
        Services.provide(ServerDataService::class.java, this)

        val env = Dotenv.load()
        val credential = Redis.Credential(
            env.get("REDIS_USERNAME"),
            env.get("REDIS_PASSWORD"),
            env.get("REDIS_ADDRESS"),
            env.get("REDIS_PORT").toInt()
        )

        Services.provide(Redis.Credential::class.java, credential)
        val redis = Services.provide(Redis::class.java, Redis(credential))

        for (clazz in findAnnotatedClasses(Controller::class)) {
            val meta = clazz.findAnnotation<Controller>() ?: continue

            if (meta.requiredPlugins.isNotEmpty() && meta.requiredPlugins.any { plugin ->
                    !Helper.plugins().isPluginEnabled(plugin)
                }) {
                logger.info("Could not enable controller: ${clazz.simpleName}. Required plugins not present.")
                return
            }

            bindModule(clazz.primaryConstructor?.call() as TerminableModule)
            logger.info("Bound controller as module: ${clazz.simpleName}")
        }

        bind(AutoCloseable {
            redis.servers().remove(server)
        })
    }

    override fun id(): String {
        return serverId
    }
}