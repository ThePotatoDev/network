package gg.tater.oneblock

import gg.tater.shared.plugin.GameServerPlugin
import gg.tater.shared.server.ServerDataService
import me.lucko.helper.Services

class OneBlockCorePlugin : GameServerPlugin() {

    override fun enable() {
        val serverType = Services.load(ServerDataService::class.java).serverType()
        initControllers(serverType, listOf("gg.tater.oneblock"))
    }
}