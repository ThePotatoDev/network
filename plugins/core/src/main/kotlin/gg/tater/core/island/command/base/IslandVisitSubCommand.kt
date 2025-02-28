package gg.tater.core.island.command.base

import gg.tater.core.island.Island
import gg.tater.core.island.IslandService
import gg.tater.core.island.command.IslandSubCommand
import gg.tater.core.island.player.IslandPlayer
import gg.tater.core.island.player.IslandPlayerService
import gg.tater.core.island.player.position.PositionDirector
import gg.tater.core.island.setting.model.IslandSettingType
import gg.tater.core.server.ServerDataService
import gg.tater.core.server.model.ServerType
import me.lucko.helper.Services
import me.lucko.helper.command.context.CommandContext
import net.luckperms.api.LuckPermsProvider
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class IslandVisitSubCommand<T : Island, K : IslandPlayer> : IslandSubCommand<T> {

    override fun id(): String {
        return "visit"
    }

    override fun handle(context: CommandContext<Player>) {
        val players: IslandPlayerService<K> = Services.load(IslandPlayerService::class.java) as IslandPlayerService<K>
        val islands: IslandService<T, K> =
            Services.load(IslandService::class.java) as IslandService<T, K>

        val server = Services.load(ServerDataService::class.java).id()

        if (context.args().size < 2) {
            context.reply("&cUsage: /island visit <player>")
            return
        }

        val perms = LuckPermsProvider.get()
        val sender = context.sender()
        val arg = context.arg(1).parseOrFail(String::class.java)

        perms.userManager.lookupUniqueId(arg).thenAcceptAsync { uuid ->
            if (uuid == null) {
                context.reply("&cPlayer does not exist.")
                return@thenAcceptAsync
            }

            val player = players.get(uuid).get()
            val islandId = player.islandId

            if (islandId == null) {
                context.reply("&cPlayer does not have an island associated with them.")
                return@thenAcceptAsync
            }

            val island = islands.getIsland(islandId).get()
            if (island == null) {
                context.reply("&cPlayer does not have an island.")
                return@thenAcceptAsync
            }

            val data = players.get(sender.uniqueId).get()
            if (data == null) {
                context.reply("&cCould not find player data.")
                return@thenAcceptAsync
            }

            if (island.getSettingValue(IslandSettingType.LOCKED) && island.getRoleFor(uuid) != Island.Role.MEMBER) {
                context.reply("&cThat island is currently locked. Only members can visit!")
                return@thenAcceptAsync
            }

            context.reply("&a&oTeleporting you to ${island.ownerName}'s island...")

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

            data.setNextServerSpawnPos(
                ServerType.ONEBLOCK_SERVER,
                PositionDirector.ISLAND_TELEPORT_DIRECTOR,
                island.spawn
            )

            players.transaction(
                data,
                onSuccess = {
                    islands.directToOccupiedServer(sender, island)
                })
        }
    }
}