package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.redis.Redis
import gg.tater.shared.island.flag.model.FlagType
import me.lucko.helper.command.context.CommandContext
import net.luckperms.api.LuckPermsProvider
import org.bukkit.entity.Player

class IslandInviteSubCommand(val redis: Redis) : IslandSubCommand {

    override fun id(): String {
        return "invite"
    }

    override fun handle(context: CommandContext<Player>) {
        val perms = LuckPermsProvider.get()
        val sender = context.sender()

        redis.players().getAsync(sender.uniqueId).thenAcceptAsync { data ->
            val island = data.islandId?.let { redis.islands()[it] }
            if (island == null) {
                context.reply("&cYou do not have an island.")
                return@thenAcceptAsync
            }

            val name = context.arg(1).parseOrFail(String::class.java)
            val target = perms.userManager.lookupUniqueId(name).join()
            if (target == null) {
                context.reply("&cCould not find player with that name.")
                return@thenAcceptAsync
            }

            if (!island.canInteract(sender.uniqueId, FlagType.INVITE_PLAYERS)) {
                context.reply("&cYou are not allowed to invite players to your island.")
                return@thenAcceptAsync
            }

            if (redis.invites().containsEntry(target, island.id)) {
                context.reply("&cThat player already has a pending invite to your island.")
                return@thenAcceptAsync
            }

            redis.invites().put(target, island.id)
            context.reply("&aYou have invited $name to your island!")
        }
    }
}