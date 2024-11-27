package gg.tater.shared.player.chat

import io.papermc.paper.event.player.AsyncChatEvent
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.promise.Promise
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer

abstract class ScopedChatPrompt(private val config: PromptConfig) : TerminableModule {

    private val cache: MutableMap<UUID, BiConsumer<String, ScopedChatPrompt>> = ExpiringMap
        .builder()
        .expirationPolicy(config.policy)
        .expiration(config.duration, config.unit)
        .asyncExpirationListener { uuid: UUID, _: BiConsumer<String, ScopedChatPrompt> ->
            val target = Bukkit.getPlayer(uuid) ?: return@asyncExpirationListener
            onExpire(target)
        }
        .build()

    class PromptConfig(
        val policy: ExpirationPolicy,
        val unit: TimeUnit,
        val duration: Long,
        val escapeIds: Set<String>,
        val messages: List<Component>
    )

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(AsyncChatEvent::class.java, EventPriority.HIGH)
            .filter(EventFilters.ignoreCancelled())
            .filter { cache.containsKey(it.player.uniqueId) }
            .handler {
                val player = it.player
                val message = LegacyComponentSerializer.legacySection().serialize(it.message())
                it.isCancelled = true
                val escaping = config.escapeIds.any { id -> message.equals(id, true) }

                if (escaping) {
                    cache.remove(player.uniqueId)
                    player.sendMessage(Component.text("You have exited the prompt", NamedTextColor.RED))
                    return@handler
                }

                handleInput(player, message)
            }
            .bindWith(consumer)

        Events.subscribe(PlayerQuitEvent::class.java, EventPriority.HIGHEST)
            .handler {
                val player = it.player
                cache.remove(player.uniqueId)
            }
            .bindWith(consumer)
    }

    protected abstract fun onExpire(target: Player)

    private fun handleInput(sender: Player, message: String): Promise<Void> {
        return Schedulers.sync().run {
            val consumer = cache[sender.uniqueId] ?: return@run
            consumer.accept(message, this)
        }
    }

    fun start(target: Player, consumer: BiConsumer<String, ScopedChatPrompt>) {
        if (cache.containsKey(target.uniqueId)) return
        for (message in config.messages) {
            target.sendMessage(message)
        }
        cache[target.uniqueId] = consumer
    }

    fun end(target: Player) {
        cache.remove(target.uniqueId)
    }
}