package gg.tater.core.controllers.island.subcommand

import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.player.PlayerService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import net.luckperms.api.LuckPermsProvider
import org.bukkit.entity.Player

class IslandJoinSubCommand(
    private val players: PlayerService = Services.load(PlayerService::class.java),
    private val islands: IslandService = Services.load(IslandService::class.java)
) : IslandSubCommand {

    override fun id(): String {
        return "join"
    }

    override fun handle(context: CommandContext<Player>) {
        val perms = LuckPermsProvider.get()
        val sender = context.sender()
        val senderId = sender.uniqueId

        players.get(senderId).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
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

            val targetData = players.get(target).get()
            val targetIsland = islands.getIslandFor(targetData)?.get()

            if (targetIsland == null) {
                context.reply("&cThat island no longer exists.")
                return@thenAcceptAsync
            }

            if (!islands.hasInvite(senderId, targetIsland).get()) {
                context.reply("&cYou do not have an invite to join that island.")
                return@thenAcceptAsync
            }

            targetIsland.setRoleFor(senderId, Island.Role.MEMBER)
            player.islandId = targetIsland.id

            players.save(player)
            islands.save(targetIsland)

            context.reply("&aYou have joined ${targetIsland.ownerName}'s island!")
        }
    }
}