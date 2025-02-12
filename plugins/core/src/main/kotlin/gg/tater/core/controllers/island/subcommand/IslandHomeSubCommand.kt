package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.island.IslandService
import gg.tater.shared.network.model.server.ServerType
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class IslandHomeSubCommand(
    private val redis: Redis,
    private val server: String,
    private val players: PlayerService = Services.load(PlayerService::class.java),
    private val islands: IslandService = Services.load(IslandService::class.java)
) : IslandSubCommand {

    override fun id(): String {
        return "home"
    }

    override fun handle(context: CommandContext<Player>) {
        val sender = context.sender()

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

            player.setSpawn(ServerType.SERVER, island.spawn)

            // If they are already set to teleport home, direct immediately
            if (player.getCurrentPositionResolver() == PlayerPositionResolver.Type.TELEPORT_ISLAND_HOME) {
                islands.directToOccupiedServer(sender, island)
                return@thenAcceptAsync
            }

            players.transaction(
                player.setPositionResolver(PlayerPositionResolver.Type.TELEPORT_ISLAND_HOME),
                onSuccess = {
                    islands.directToOccupiedServer(sender, island)
                })
        }
    }
}