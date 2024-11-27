package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.redis.Redis
import gg.tater.shared.island.Island
import me.lucko.helper.command.context.CommandContext
import net.luckperms.api.LuckPermsProvider
import org.bukkit.entity.Player

class IslandJoinSubCommand(private val redis: Redis) : IslandSubCommand {

    override fun id(): String {
        return "join"
    }

    override fun handle(context: CommandContext<Player>) {
        val perms = LuckPermsProvider.get()
        val sender = context.sender()
        val senderId = sender.uniqueId

        redis.players().getAsync(sender.uniqueId).thenAcceptAsync { data ->
            val island = data.islandId?.let { redis.islands()[it] }
            if (island != null) {
                context.reply("&cYou already have an island.")
                return@thenAcceptAsync
            }

            val name = context.arg(1).parseOrFail(String::class.java)
            val target = perms.userManager.lookupUniqueId(name).join()
            if (target == null) {
                context.reply("&cCould not find player with that name.")
                return@thenAcceptAsync
            }

            val targetData = redis.players()[target]
            val targetIsland = redis.islands()[targetData?.islandId]

            if (targetData == null || targetIsland == null) {
                context.reply("&cThat island no longer exists.")
                return@thenAcceptAsync
            }

            if (!redis.invites().containsEntry(senderId, targetIsland.id)) {
                context.reply("&cYou do not have an invite to join that island.")
                return@thenAcceptAsync
            }

            targetIsland.setRoleFor(senderId, Island.Role.MEMBER)
            data.islandId = targetIsland.id

            redis.players()[senderId] = data
            redis.islands()[targetIsland.id] = targetIsland

            context.reply("&aYou have joined ${targetIsland.ownerName}'s island!")
        }
    }
}