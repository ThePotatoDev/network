package gg.tater.oneblock

import gg.tater.core.Json
import gg.tater.core.Mappings
import gg.tater.core.island.cache.IslandWorldCacheController
import gg.tater.core.player.auction.AuctionHouseController
import gg.tater.core.player.economy.EconomyController
import gg.tater.core.plugin.GameServerPlugin
import gg.tater.core.server.ServerDataService
import gg.tater.core.server.model.GameModeType
import gg.tater.core.server.model.ONEBLOCK_GAMEMODE_SERVERS
import gg.tater.oneblock.island.controllers.OneBlockExperienceController
import gg.tater.oneblock.island.OneBlockIsland
import gg.tater.oneblock.island.controllers.OneBlockIslandController
import gg.tater.oneblock.island.controllers.OneBlockIslandService
import gg.tater.oneblock.planet.PlanetController
import gg.tater.oneblock.player.OneBlockPlayer
import gg.tater.oneblock.player.OneBlockPlayerService
import gg.tater.oneblock.scoreboard.OneBlockMainScoreboard
import gg.tater.oneblock.spawn.OneBlockSpawnController
import me.lucko.helper.Services

class OneBlockCorePlugin : GameServerPlugin() {

    override fun enable() {
        Mappings.loadMappings()
        Json.registerAdapters()

        val serverType = Services.load(ServerDataService::class.java).serverType()

        useController(OneBlockIslandService())
        useController(OneBlockPlayerService())

        // Bind non-data related controllers if on a OneBlock server
        if (ONEBLOCK_GAMEMODE_SERVERS.contains(serverType)) {
            useController(OneBlockExperienceController())
            useController(IslandWorldCacheController<OneBlockIsland, OneBlockPlayer>())

            useController(EconomyController(GameModeType.ONEBLOCK))
            useController(AuctionHouseController(GameModeType.ONEBLOCK))

            useController(OneBlockIslandController())
            useController(OneBlockSpawnController())
            useController(PlanetController())

            useController(OneBlockMainScoreboard(this))
        }
    }
}