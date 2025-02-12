package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.message.placement.IslandPlacementRequest
import gg.tater.shared.network.model.server.ServerType
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player
import java.util.*

class IslandCreateSubCommand(private val redis: Redis) : IslandSubCommand {

    override fun id(): String {
        return "create"
    }

    override fun handle(context: CommandContext<Player>) {
        val players: PlayerService = Services.load(PlayerService::class.java)
        val islands: IslandService = Services.load(IslandService::class.java)

        val sender = context.sender()
        val uuid = sender.uniqueId

        players.get(uuid).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
            if (island != null) {
                context.reply("&cYou already have an island.")
                return@thenAcceptAsync
            }

            val server = redis.getServer(ServerType.SERVER)
            if (server == null) {
                context.reply("&cCould not find server.")
                return@thenAcceptAsync
            }

            context.reply("&a&oRequesting your island...")

            val newIsland = Island(UUID.randomUUID(), uuid, sender.name)
            newIsland.currentServerId = server.id
            islands.save(newIsland)

            player.islandId = newIsland.id
            player.setDefaultSpawn(ServerType.SERVER)

            players.transaction(
                player.setPositionResolver(PlayerPositionResolver.Type.TELEPORT_ISLAND_HOME),
                onSuccess = {
                    redis.publish(
                        IslandPlacementRequest(
                            server.id,
                            sender.uniqueId,
                            newIsland.id,
                            sender.name,
                            true
                        )
                    )
                })
        }
    }
}