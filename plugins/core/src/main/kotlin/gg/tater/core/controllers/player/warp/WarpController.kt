package gg.tater.core.controllers.player.warp

import gg.tater.shared.player.warp.WarpGui
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

class WarpController(private val redis: Redis, private val server: String) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
                WarpGui(sender, redis, server).open()
            }
            .registerAndBind(consumer, "warps", "warp")
    }
}