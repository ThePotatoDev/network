package gg.tater.core.controllers.player.message

import gg.tater.core.controllers.player.message.listener.PlayerPrivateMessageRequestListener
import gg.tater.core.controllers.player.message.listener.PlayerPrivateMessageResponseListener
import gg.tater.shared.player.message.PlayerPrivateMessageRequest
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.luckperms.api.LuckPermsProvider

class PlayerMessageController(private val redis: Redis) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
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

                    val data = redis.players()[targetId]
                    if (data == null) {
                        it.reply("&cCould not find player data.")
                        return@thenAcceptAsync
                    }

                    if (!data.online) {
                        it.reply("&cThat player is not online.")
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

                redis.targets().getAsync(sender.uniqueId).thenAcceptAsync { targetId ->
                    if (targetId == null) {
                        it.reply("&cYou have no one to reply to.")
                        return@thenAcceptAsync
                    }

                    val data = redis.players()[targetId]
                    if (data == null) {
                        it.reply("&cCould not find player data.")
                        return@thenAcceptAsync
                    }

                    if (!data.online) {
                        it.reply("&cThat player is not online.")
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