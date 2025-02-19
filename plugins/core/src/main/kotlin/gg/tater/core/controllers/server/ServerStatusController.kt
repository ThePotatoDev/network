package gg.tater.core.controllers.server

import gg.tater.shared.annotation.Controller
import gg.tater.shared.network.Agones
import gg.tater.shared.network.server.ServerDataService
import gg.tater.shared.network.server.ServerState
import gg.tater.shared.redis.Redis
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit

@Controller(
    id = "server-status-controller"
)
class ServerStatusController :
    TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        val actions = Services.load(Agones::class.java)
        val id = Services.load(ServerDataService::class.java).id()
        val redis = Services.load(Redis::class.java)

        Schedulers.sync().runRepeating(Runnable {
            val runtime = Runtime.getRuntime()

            if (Bukkit.getOnlinePlayers().isEmpty()) {
                actions.ready()
            }

            val server = redis.servers()[id] ?: return@Runnable
            actions.health()

            if (Bukkit.getOnlinePlayers().isEmpty()) {
                server.state = ServerState.READY
            } else {
                actions.allocate()
                server.state = ServerState.ALLOCATED
            }

            server.maxMemory = runtime.maxMemory()
            server.freeMemory = runtime.freeMemory()
            server.players = Bukkit.getOnlinePlayers().size
            redis.servers()[id] = server
        }, 20L, 20L).bindWith(consumer)

        consumer.bind(AutoCloseable {
            actions.shutdown()
            redis.semaphores(id).delete()
        })
    }
}