package gg.tater.core.player.teleport

import gg.tater.core.annotation.Controller
import gg.tater.core.player.teleport.message.TeleportRequestMessage
import gg.tater.core.redis.Redis
import gg.tater.core.server.ServerDataService
import gg.tater.core.server.model.GameModeType
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit

@Controller(
    id = "teleport-controller"
)
class TeleportController(mode: GameModeType) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        val redis = Services.load(Redis::class.java)
        val server = Services.load(ServerDataService::class.java).id()

        redis.listen<TeleportRequestMessage> {
            val request = it.request
            val player = Bukkit.getPlayer(request.targetId) ?: return@listen

            if (request.state == TeleportRequest.TeleportState.PENDING) {

            }
        }

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

                if (it.args().size != 1) {
                    it.reply("&cUsage: /tpa <player>")
                    return@handler
                }

                val target = it.arg(0).parseOrFail(String::class.java)

            }
            .registerAndBind(consumer, "teleport", "tp", "tpa")

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

            }
            .registerAndBind(consumer, "tpaccept")

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

            }
            .registerAndBind(consumer, "tpdeny")
    }
}