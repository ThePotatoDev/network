package gg.tater.shared.island.command.base

import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.command.IslandSubCommand
import gg.tater.shared.island.message.IslandDeleteRequest
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandDeleteSubCommand<T : Island> : IslandSubCommand<T> {

    override fun id(): String {
        return "delete"
    }

    override fun handle(context: CommandContext<Player>) {
        val redis = Services.load(Redis::class.java)
        val players: PlayerService = Services.load(PlayerService::class.java)
        val islands: IslandService<T> =
            Services.load(IslandService::class.java) as IslandService<T>

        val sender = context.sender()
        val uuid = sender.uniqueId

        players.get(uuid).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            val role = island.getRoleFor(uuid)
            if (role != Island.Role.OWNER) {
                context.reply("&cOnly the island owner can delete the island.")
                return@thenAcceptAsync
            }

            context.reply("&cYour island has been deleted.")

            val id = island.id
            val server = island.currentServerId

            islands.transaction(
                { map -> map.remove(island.id) },
                onSuccess = {
                    redis.publish(IslandDeleteRequest(id, server))
                })
        }
    }
}