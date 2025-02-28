package gg.tater.core.island.command.base

import gg.tater.core.island.Island
import gg.tater.core.island.IslandService
import gg.tater.core.island.command.IslandSubCommand
import gg.tater.core.island.player.IslandPlayer
import gg.tater.core.island.player.IslandPlayerService
import gg.tater.core.island.player.position.PositionDirector
import gg.tater.core.server.ServerDataService
import gg.tater.core.server.model.ServerType
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class IslandHomeSubCommand<T : Island, K : IslandPlayer>(private val islandServerType: ServerType) :
    IslandSubCommand<T> {

    override fun id(): String {
        return "home"
    }

    override fun handle(context: CommandContext<Player>) {
        val sender = context.sender()
        val players: IslandPlayerService<K> = Services.load(IslandPlayerService::class.java) as IslandPlayerService<K>
        val islands: IslandService<T, K> =
            Services.load(IslandService::class.java) as IslandService<T, K>
        val server = Services.load(ServerDataService::class.java).id()

        players.get(sender.uniqueId).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            context.reply("&a&oTeleporting you to your island...")

            // If player is on the same server as the island, teleport them directly
            val currentServerId = island.currentServerId
            if (currentServerId != null && currentServerId == server) {
                val spawn = island.spawn
                sender.teleportAsync(
                    Location(
                        Bukkit.getWorld(island.id.toString()),
                        spawn.x,
                        spawn.y,
                        spawn.z,
                        spawn.yaw,
                        spawn.pitch
                    )
                )
                return@thenAcceptAsync
            }

            players.transaction(
                player.setNextServerSpawnPos(islandServerType, PositionDirector.ISLAND_TELEPORT_DIRECTOR, island.spawn),
                onSuccess = {
                    islands.directToOccupiedServer(sender, island)
                })
        }
    }
}