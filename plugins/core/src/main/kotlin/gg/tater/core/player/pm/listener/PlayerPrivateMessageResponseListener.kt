package gg.tater.core.player.pm.listener

import gg.tater.core.player.pm.PrivateMessageService
import gg.tater.core.player.pm.model.PlayerPrivateMessageResponse
import gg.tater.core.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit

class PlayerPrivateMessageResponseListener(private val redis: Redis) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        redis.listen<PlayerPrivateMessageResponse> {
            val messages = Services.load(PrivateMessageService::class.java)

            val sender = Bukkit.getPlayer(it.senderName) ?: return@listen
            val target = it.targetName
            val message = it.message

            // Store their conversation for 1 minute
            messages.set(sender.uniqueId, it.targetId)
            messages.set(it.targetId, sender.uniqueId)

            sender.sendMessage(
                Component.text("[", NamedTextColor.DARK_GRAY)
                    .append(Component.text("Me", NamedTextColor.YELLOW))
                    .append(Component.text(" -> ", NamedTextColor.GRAY))
                    .append(Component.text(target, NamedTextColor.YELLOW))
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(message, NamedTextColor.WHITE))
            )
        }
    }
}