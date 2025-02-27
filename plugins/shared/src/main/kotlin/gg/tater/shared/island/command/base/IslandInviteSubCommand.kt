package gg.tater.shared.island.command.base

import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.command.IslandSubCommand
import gg.tater.shared.island.flag.model.FlagType
import gg.tater.shared.island.player.IslandPlayer
import gg.tater.shared.island.player.IslandPlayerService
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import net.luckperms.api.LuckPermsProvider
import org.bukkit.entity.Player

class IslandInviteSubCommand<T : Island, K: IslandPlayer> :
    IslandSubCommand<T> {

    override fun id(): String {
        return "invite"
    }

    override fun handle(context: CommandContext<Player>) {
        val perms = LuckPermsProvider.get()
        val sender = context.sender()

        val players: IslandPlayerService<K> = Services.load(IslandPlayerService::class.java) as IslandPlayerService<K>
        val islands: IslandService<T, K> =
            Services.load(IslandService::class.java) as IslandService<T, K>

        players.get(sender.uniqueId).thenAcceptAsync { player ->
            val island = islands.getIslandFor(player)?.get()
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

            if (islands.hasInvite(target, island).get()) {
                context.reply("&cThat player already has a pending invite to your island.")
                return@thenAcceptAsync
            }

            islands.addInvite(target, island)
            context.reply("&aYou have invited $name to your island!")
        }
    }
}