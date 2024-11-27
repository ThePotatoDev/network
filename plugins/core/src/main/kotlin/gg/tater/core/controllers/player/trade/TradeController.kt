package gg.tater.core.controllers.player.trade

import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

class TradeController(private val redis: Redis) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
            }
            .registerAndBind(consumer, "trade")
    }
}