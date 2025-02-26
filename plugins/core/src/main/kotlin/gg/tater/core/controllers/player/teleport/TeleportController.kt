package gg.tater.core.controllers.player.teleport

import gg.tater.shared.annotation.Controller
import gg.tater.shared.network.server.ServerDataService
import gg.tater.shared.network.server.ServerType
import gg.tater.shared.player.combat.CombatService
import gg.tater.shared.player.teleport.TeleportRequest
import gg.tater.shared.player.teleport.message.TeleportRequestMessage
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Bukkit

@Controller(
    id = "teleport-controller",
    ignoredBinds = [ServerType.HUB]
)
class TeleportController : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        val combat = Services.load(CombatService::class.java)
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

                if (combat.isInCombat(sender.uniqueId)) {
                    it.reply("&cYou cannot teleport while in combat!")
                    return@handler
                }

                val target = it.arg(0).parseOrFail(String::class.java)

            }
            .registerAndBind(consumer, "teleport", "tp", "tpa")

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

                if (combat.isInCombat(sender.uniqueId)) {
                    it.reply("&cYou cannot teleport while in combat!")
                    return@handler
                }

            }
            .registerAndBind(consumer, "tpaccept")

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

                if (combat.isInCombat(sender.uniqueId)) {
                    it.reply("&cYou cannot teleport while in combat!")
                    return@handler
                }
            }
            .registerAndBind(consumer, "tpdeny")
    }
}