package gg.tater.core.controllers.player.pm.listener

import gg.tater.shared.player.message.PlayerPrivateMessageResponse
import gg.tater.shared.redis.Redis
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import java.util.concurrent.TimeUnit

class PlayerPrivateMessageResponseListener(private val redis: Redis) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        redis.listen<PlayerPrivateMessageResponse> {
            val sender = Bukkit.getPlayer(it.senderName) ?: return@listen
            val target = it.targetName
            val message = it.message

            // Store their conversation for 1 minute
            redis.targets().fastPut(sender.uniqueId, it.targetId, 1L, TimeUnit.MINUTES)
            redis.targets().fastPut(it.targetId, sender.uniqueId, 1L, TimeUnit.MINUTES)

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