package gg.tater.core.player.pm.listener

import gg.tater.core.player.pm.model.PlayerPrivateMessageRequest
import gg.tater.core.player.pm.model.PlayerPrivateMessageResponse
import gg.tater.core.redis.Redis
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit

class PlayerPrivateMessageRequestListener(private val redis: Redis) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        redis.listen<PlayerPrivateMessageRequest> {
            val target = Bukkit.getPlayer(it.targetId) ?: return@listen
            val message = it.message
            val sender = it.senderName

            target.sendMessage(
                Component.text("[", NamedTextColor.DARK_GRAY)
                    .append(Component.text(sender, NamedTextColor.YELLOW))
                    .append(Component.text(" -> ", NamedTextColor.GRAY))
                    .append(Component.text("Me", NamedTextColor.YELLOW))
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(message, NamedTextColor.WHITE))
            )

            redis.publish(PlayerPrivateMessageResponse(it))
        }
    }
}