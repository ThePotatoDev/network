package gg.tater.core.controllers.player.chat

import gg.tater.shared.MINI_MESSAGE
import gg.tater.shared.player.PlayerService
import gg.tater.shared.player.chat.color.ChatColorGui
import gg.tater.shared.player.chat.message.ChatMessagePart
import gg.tater.shared.player.chat.message.ChatMessageRequest
import gg.tater.shared.redis.Redis
import io.papermc.paper.event.player.AsyncChatEvent
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.luckperms.api.LuckPermsProvider
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority

class PlayerChatController(
    private val redis: Redis,
    private val players: PlayerService = Services.load(PlayerService::class.java)
) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        val perms = LuckPermsProvider.get()

        redis.listen<ChatMessageRequest> {
            var targets = it.targets

            // If targets are null, it's a global message
            if (targets == null) {
                targets = Bukkit.getOnlinePlayers().map { player -> player.uniqueId }.toMutableSet()
            }

            for (target in targets) {
                val player = Bukkit.getPlayer(target) ?: continue
                if (it.permission != null && !player.hasPermission(it.permission!!)) continue

                val component: Component =
                    LegacyComponentSerializer.legacyAmpersand().deserialize(it.getPart(ChatMessagePart.PREFIX))
                        .append(
                            LegacyComponentSerializer.legacyAmpersand()
                                .deserialize("&e${it.getPart(ChatMessagePart.NAME)}&f: ")
                        )
                        .append(
                            if (it.hasSpecialColor()) MINI_MESSAGE.deserialize(
                                "<gradient:${it.getPart(ChatMessagePart.START_CHAT_COLOR)}:${it.getPart(ChatMessagePart.END_CHAT_COLOR)}>${
                                    it.getPart(
                                        ChatMessagePart.TEXT
                                    )
                                }</gradient>"
                            )
                            else PlainTextComponentSerializer.plainText()
                                .deserialize(it.getPart(ChatMessagePart.TEXT))
                                .color(NamedTextColor.WHITE)
                        )

                player.sendMessage(component)
            }
        }

        Events.subscribe(AsyncChatEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                val user = perms.userManager.getUser(player.uniqueId)
                val group = perms.groupManager.getGroup(user?.primaryGroup ?: "default")

                it.isCancelled = true

                if (user == null || group == null) {
                    player.sendMessage("Error handling chat message.")
                    return@handler
                }

                val request = ChatMessageRequest(null, null)

                val prefix = group.cachedData.metaData.prefix
                val text = LegacyComponentSerializer.legacyAmpersand().serialize(it.message())

                request.setPart(ChatMessagePart.NAME, player.name)
                request.setPart(ChatMessagePart.TEXT, text)
                request.setPart(ChatMessagePart.PREFIX, prefix!!)

                val data = players.get(player.uniqueId).get()

                if (data.chatColor != null) {
                    val color = data.chatColor!!
                    request.setPart(ChatMessagePart.START_CHAT_COLOR, color.startColor)
                    request.setPart(ChatMessagePart.END_CHAT_COLOR, color.endColor)
                }

                redis.publish(request)
            }
            .bindWith(consumer)

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
                players.get(sender.uniqueId).thenAccept { player -> ChatColorGui(sender, player).open() }
            }
            .registerAndBind(consumer, "chatcolor", "cc")

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
            }
            .registerAndBind(consumer, "tags", "tag")
    }
}