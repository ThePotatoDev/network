package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.redis.Redis
import gg.tater.shared.island.Island
import gg.tater.shared.island.message.IslandDeleteRequest
import me.lucko.helper.command.context.CommandContext
import org.bukkit.entity.Player

class IslandDeleteSubCommand(private val redis: Redis, private val server: String) : IslandSubCommand {

    override fun id(): String {
        return "delete"
    }

    override fun handle(context: CommandContext<Player>) {
        val sender = context.sender()
        val uuid = sender.uniqueId

        redis.players().getAsync(uuid).thenAcceptAsync { player ->
            val island = player.islandId?.let { redis.islands()[it] }
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

            redis.deleteIsland(island).thenRun {
                redis.publish(IslandDeleteRequest(id, server))
            }
        }
    }
}