package gg.tater.core.player.pm

import gg.tater.core.annotation.Controller
import gg.tater.core.player.pm.listener.PlayerPrivateMessageRequestListener
import gg.tater.core.player.pm.listener.PlayerPrivateMessageResponseListener
import gg.tater.core.player.pm.model.PlayerPrivateMessageRequest
import gg.tater.core.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import net.luckperms.api.LuckPermsProvider
import org.redisson.api.RFuture
import java.util.*
import java.util.concurrent.TimeUnit

@Controller(
    id = "private-message-controller"
)
class PlayerPrivateMessageController : PrivateMessageService {

    private val redis = Services.load(Redis::class.java)

    override fun get(uuid: UUID): RFuture<UUID> {
        return redis.client.getMapCache<UUID, UUID>(PrivateMessageService.MESSAGE_TARGET_MAP_NAME)
            .getAsync(uuid)
    }

    override fun set(uuid: UUID, targetId: UUID): RFuture<Boolean> {
        return redis.client.getMapCache<UUID, UUID>(PrivateMessageService.MESSAGE_TARGET_MAP_NAME)
            .fastPutAsync(uuid, targetId, 1L, TimeUnit.MINUTES)
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(PrivateMessageService::class.java, this)
        val perms = LuckPermsProvider.get()

        consumer.bindModule(PlayerPrivateMessageRequestListener(redis))
        consumer.bindModule(PlayerPrivateMessageResponseListener(redis))

        Commands.create()
            .assertPlayer()
            .handler {
                if (it.args().size < 2) {
                    it.reply("&cUsage: /msg <player> <message>")
                    return@handler
                }

                val sender = it.sender()
                val target = it.arg(0).parseOrFail(String::class.java)
                val message = it.args().drop(1).joinToString(" ")

                perms.userManager.lookupUniqueId(target).thenAcceptAsync { targetId ->
                    if (targetId == null) {
                        it.reply("&cThat player does not exist.")
                        return@thenAcceptAsync
                    }

                    redis.publish(PlayerPrivateMessageRequest(sender.name, sender.uniqueId, target, targetId, message))
                }
            }
            .registerAndBind(consumer, "msg", "message", "tell", "w", "whisper")

        Commands.create()
            .assertPlayer()
            .handler {
                if (it.args().size < 1) {
                    it.reply("&cUsage: /reply <message>")
                    return@handler
                }

                val sender = it.sender()
                val message = it.args().joinToString(" ")

                get(sender.uniqueId).thenAcceptAsync { targetId ->
                    if (targetId == null) {
                        it.reply("&cYou have no one to reply to.")
                        return@thenAcceptAsync
                    }

                    val username = perms.userManager.lookupUsername(targetId).join()
                    redis.publish(
                        PlayerPrivateMessageRequest(
                            sender.name,
                            sender.uniqueId,
                            username,
                            targetId,
                            message
                        )
                    )
                }
            }
            .registerAndBind(consumer, "reply", "r")
    }
}