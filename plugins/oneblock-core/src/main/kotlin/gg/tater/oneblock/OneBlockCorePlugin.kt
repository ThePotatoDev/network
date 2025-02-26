package gg.tater.oneblock

import gg.tater.oneblock.island.controllers.OneBlockIslandController
import gg.tater.oneblock.island.controllers.OneBlockIslandServiceController
import gg.tater.oneblock.planet.PlanetController
import gg.tater.oneblock.player.OneBlockPlayerServiceController
import gg.tater.oneblock.spawn.OneBlockSpawnController
import gg.tater.shared.player.auction.AuctionHouseController
import gg.tater.shared.plugin.GameServerPlugin
import gg.tater.shared.server.ServerDataService
import gg.tater.shared.server.model.GameModeType
import gg.tater.shared.server.model.ONEBLOCK_GAMEMODE_SERVERS
import me.lucko.helper.Services

class OneBlockCorePlugin : GameServerPlugin() {

    override fun enable() {
        val serverType = Services.load(ServerDataService::class.java).serverType()

        useController(null, OneBlockIslandServiceController::class)
        useController(null, OneBlockPlayerServiceController::class)

        // Bind non-data related controllers if on a OneBlock server
        if (ONEBLOCK_GAMEMODE_SERVERS.contains(serverType)) {
            useController(GameModeType.ONEBLOCK, AuctionHouseController::class)

            useController(null, OneBlockIslandController::class)
            useController(null, OneBlockSpawnController::class)
            useController(null, PlanetController::class)
        }
    }
}