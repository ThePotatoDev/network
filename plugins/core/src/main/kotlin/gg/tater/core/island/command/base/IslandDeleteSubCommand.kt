package gg.tater.core.island.command.base

import gg.tater.core.island.Island
import gg.tater.core.island.IslandService
import gg.tater.core.island.command.IslandSubCommand
import gg.tater.core.island.message.IslandDeleteRequest
import gg.tater.core.island.player.IslandPlayer
import gg.tater.core.island.player.IslandPlayerService
import gg.tater.core.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandDeleteSubCommand<T : Island, K: IslandPlayer> : IslandSubCommand<T> {

    override fun id(): String {
        return "delete"
    }

    override fun handle(context: CommandContext<Player>) {
        val redis = Services.load(Redis::class.java)
        val players: IslandPlayerService<K> = Services.load(IslandPlayerService::class.java) as IslandPlayerService<K>
        val islands: IslandService<T, K> =
            Services.load(IslandService::class.java) as IslandService<T, K>

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