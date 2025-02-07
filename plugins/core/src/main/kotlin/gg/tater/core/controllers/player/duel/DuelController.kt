package gg.tater.core.controllers.player.duel

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI
import com.infernalsuite.aswm.loaders.redis.RedisLoader
import gg.tater.shared.player.duel.DuelService
import gg.tater.shared.player.duel.model.DuelRequest
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.terminable.TerminableConsumer
import java.util.*

class DuelController(private val redis: Redis, private val credential: Redis.Credential) : DuelService {

    private lateinit var api: AdvancedSlimePaperAPI

    override fun setup(consumer: TerminableConsumer) {
        this.api = AdvancedSlimePaperAPI.instance()
        val loader =
            RedisLoader("redis://:${credential.password}@${credential.address}:${credential.port}")

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

                if (it.args().isEmpty()) {
                    it.reply("&cSpecify a player to send a duel request to.")
                    return@handler
                }
            }
            .registerAndBind(consumer, "duel", "duels")
    }

    override fun startDuel(request: DuelRequest) {
        TODO("Not yet implemented")
    }

    override fun endDuel(request: DuelRequest) {
        TODO("Not yet implemented")
    }

    override fun isInDuel(uuid: UUID): Boolean {
        TODO("Not yet implemented")
    }
}