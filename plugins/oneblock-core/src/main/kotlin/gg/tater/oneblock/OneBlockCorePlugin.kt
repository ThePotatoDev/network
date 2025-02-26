package gg.tater.oneblock

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import gg.tater.shared.plugin.GameServerPlugin
import gg.tater.shared.redis.Redis
import gg.tater.shared.server.ServerDataService
import me.lucko.helper.Services

class OneBlockCorePlugin : GameServerPlugin() {

    override fun enable() {
        val api = AdvancedSlimePaperAPI.instance()
        val serverType = Services.load(ServerDataService::class.java).serverType()
        initControllers(serverType)

        val credential = Services.load(Redis.Credential::class.java)
    }
}