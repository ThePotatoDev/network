package gg.tater.shared.player.warp

import gg.tater.shared.annotation.Controller
import gg.tater.shared.server.model.ServerType
import gg.tater.shared.player.warp.gui.WarpGui
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer

@Controller(
    id = "warp-controller",
    ignoredBinds = [ServerType.HUB]
)
class WarpController : WarpService {

    private val redis = Services.load(Redis::class.java)

    override fun setup(consumer: TerminableConsumer) {
        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
                WarpGui(sender, redis).open()
            }
            .registerAndBind(consumer, "warps", "warp")
    }
}