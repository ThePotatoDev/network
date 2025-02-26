package gg.tater.oneblock

import gg.tater.oneblock.island.controllers.OneBlockIslandController
import gg.tater.oneblock.planet.PlanetController
import gg.tater.oneblock.player.OneBlockPlayerController
import gg.tater.oneblock.spawn.OneBlockSpawnController
import gg.tater.shared.player.auction.AuctionHouseController
import gg.tater.shared.plugin.GameServerPlugin
import gg.tater.shared.server.model.GameModeType

class OneBlockCorePlugin : GameServerPlugin() {

    override fun enable() {
        useController(GameModeType.ONEBLOCK, AuctionHouseController::class)
        useController(null, OneBlockIslandController::class)
        useController(null, OneBlockPlayerController::class)
        useController(null, OneBlockSpawnController::class)
        useController(null, PlanetController::class)
    }
}