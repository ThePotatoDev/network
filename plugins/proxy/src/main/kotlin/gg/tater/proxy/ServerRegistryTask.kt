package gg.tater.proxy

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.tater.shared.network.Agones
import gg.tater.shared.network.ProxyDataModel
import gg.tater.shared.network.server.ServerDataModel
import gg.tater.shared.network.server.ServerType
import gg.tater.shared.redis.Redis
import io.github.cdimascio.dotenv.Dotenv
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CustomObjectsApi
import io.kubernetes.client.util.Config
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class ServerRegistryTask(
    dir: Path,
    env: Dotenv,
    private val proxy: ProxyServer,
    private val redis: Redis,
    private val actions: Agones,
    private val data: ProxyDataModel,
    private val logger: Logger
) {

    private companion object {
        const val GROUP = "agones.dev"
        const val VERSION = "v1"
        const val NAMESPACE = "default"
        const val PLURAL = "gameservers"
    }

    private val removals: MutableSet<String> = Collections.newSetFromMap(
        ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.CREATED)
            .expiration(5L, TimeUnit.SECONDS)
            .build()
    )

    init {
        val client: ApiClient =
            Config.fromConfig(if (env.get("ENV").equals("dev")) "$dir/config_dev" else "$dir/config_local")
        Configuration.setDefaultApiClient(client)
        val api = CustomObjectsApi()

        proxy.scheduler.buildTask(this, Runnable {
            actions.health()

            data.players = proxy.playerCount
            redis.proxy().set(data)

            // Agones status handling
            if (proxy.playerCount > 0) {
                actions.allocate()
            } else {
                actions.ready()
            }

            // K8's api to find active nodes & register them to the proxy
            // List all game servers in the default namespace
            val servers = api.listNamespacedCustomObject(
                GROUP,
                VERSION,
                NAMESPACE,
                PLURAL,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ) as Map<*, *>

            // Extract and print the game server details
            val items = servers["items"] as List<Map<*, *>>
            for (item in items) {
                val metadata = item["metadata"] as Map<*, *>
                val status = item["status"] as Map<*, *>
                val address = status["address"] as String?
                val ports = status["ports"] as List<Map<*, *>>? ?: continue
                val state = status["state"] as String? ?: continue
                val name = metadata["name"] as String? ?: continue

                // Do not register proxy servers
                if (name.contains("proxy")) continue

                if (state == "Ready" && proxy.getServer(name).isEmpty && !removals.contains(name)) {
                    val info = ServerInfo(
                        name, InetSocketAddress(
                            address, ports.firstOrNull()?.get("port")
                                .toString()
                                .split(".")[0]
                                .toInt()
                        )
                    )

                    proxy.registerServer(info)
                    logger.info("Registered $name")

                    val type = ServerType.valueOf(name.split("-")[0].uppercase())
                    redis.servers().putIfAbsentAsync(name, ServerDataModel(name, type))
                    return@Runnable
                }
            }

            for (server in proxy.allServers) {
                server.ping().whenComplete { _, throwable ->
                    // Server is alive
                    if (throwable == null) {
                        return@whenComplete
                    }

                    val info = server.serverInfo
                    val name = info.name

                    proxy.unregisterServer(info)
                    removals.add(name)
                    redis.servers().removeAsync(name)

                    logger.info("Unregistered $name")
                }
            }
        }).repeat(Duration.ofSeconds(1L)).schedule()
    }

}