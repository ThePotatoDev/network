package gg.tater.core.island.player

import gg.tater.core.island.cache.isIslandWorld
import gg.tater.core.island.cache.toIslandId
import gg.tater.core.island.player.position.PositionDirector
import gg.tater.core.island.player.position.SpawnPositionData
import gg.tater.core.position.WrappedPosition
import gg.tater.core.server.ServerDataService
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.redisson.api.RFuture
import java.util.*

interface IslandPlayerService<T : IslandPlayer> : TerminableModule {

    fun compute(name: String, uuid: UUID): RFuture<T>

    fun get(uuid: UUID): RFuture<T>

    fun save(data: T): RFuture<T>

    fun transaction(data: T, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})

    fun registerPositionListeners(consumer: TerminableConsumer) {
        val serverType = Services.load(ServerDataService::class.java).serverType()

        Events.subscribe(PlayerQuitEvent::class.java, EventPriority.HIGHEST)
            .handler {
                val quitter = it.player
                val world = quitter.world
                val position = WrappedPosition(quitter.location)

                get(quitter.uniqueId).thenAccept { player ->
                    if (player == null) return@thenAccept

                    // If player is in an island world
                    if (world.isIslandWorld()) {
                        player.setServerSpawnPos(
                            serverType,
                            PositionDirector.ISLAND_TELEPORT_DIRECTOR,
                            position,
                            mutableMapOf(SpawnPositionData.ISLAND_ID_META_KEY to world.toIslandId().toString())
                        )
                    } else {
                        player.setServerSpawnPos(
                            serverType,
                            PositionDirector.RESPAWN,
                            position,
                            mutableMapOf(SpawnPositionData.WORLD_NAME_META_KEY to world.name)
                        )
                    }

                    save(player)
                }
            }
            .bindWith(consumer)

        Events.subscribe(PlayerJoinEvent::class.java, EventPriority.HIGHEST)
            .handler {
                val joiner = it.player

                get(joiner.uniqueId).thenAccept { player ->
                    if (player == null) return@thenAccept

                    val spawnData = player.getServerSpawnPos(serverType) ?: return@thenAccept

                    val director = spawnData.director
                    val position = spawnData.position

                    // Teleport them delayed
                    Schedulers.sync().runLater({
                        when (director) {
                            PositionDirector.WORLD_TELEPORT_DIRECTOR -> {
                                val world = Bukkit.getWorld("world") ?: return@runLater
                                joiner.teleportAsync(
                                    Location(
                                        world,
                                        position.x,
                                        position.y,
                                        position.z,
                                        position.yaw,
                                        position.pitch
                                    )
                                )
                            }

                            PositionDirector.ISLAND_TELEPORT_DIRECTOR -> {
                                val islandId =
                                    spawnData.getMetaValue(SpawnPositionData.ISLAND_ID_META_KEY) ?: return@runLater
                                val world = Bukkit.getWorld(islandId) ?: return@runLater
                                joiner.teleportAsync(
                                    Location(
                                        world,
                                        position.x,
                                        position.y,
                                        position.z,
                                        position.yaw,
                                        position.pitch
                                    )
                                )
                            }

                            PositionDirector.RESPAWN -> {
                                val worldName =
                                    spawnData.getMetaValue(SpawnPositionData.WORLD_NAME_META_KEY) ?: return@runLater
                                // Check if the world name is of uuid type (it is an island world)
                            }
                        }
                    }, 2L)
                }
            }
            .bindWith(consumer)
    }
}