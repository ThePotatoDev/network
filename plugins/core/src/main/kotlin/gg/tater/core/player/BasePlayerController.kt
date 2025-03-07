package gg.tater.core.player

import gg.tater.core.annotation.Controller
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@Controller(
    id = "player-controller"
)
class BasePlayerController : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(PlayerJoinEvent::class.java, EventPriority.HIGHEST)
            .handler {
                it.joinMessage(null)
            }
            .bindWith(consumer)

        Events.subscribe(PlayerQuitEvent::class.java, EventPriority.HIGHEST)
            .handler {
                it.quitMessage(null)
            }
            .bindWith(consumer)

        Commands.create()
            .assertPermission("server.msgraw")
            .handler {
                if (it.args().size < 1) {
                    it.reply("&cUsage: /msgraw <target> <msg>")
                    return@handler
                }

                val target = it.arg(0).parseOrFail(Player::class.java)
                val message = it.args().drop(1).joinToString(" ")

                target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message))
            }
            .registerAndBind(consumer, "msgraw")

        Commands.create()
            .assertPlayer()
            .assertPermission("server.heal")
            .handler {
                val sender = it.sender()
                sender.health = 20.0
                sender.foodLevel = 20
                it.reply("&aYou have been healed")
            }
            .registerAndBind(consumer, "heal")

        Commands.create()
            .assertPlayer()
            .assertPermission("server.gamemode")
            .handler {
                val sender = it.sender()
                val label = it.label()

                if (label.equals("gmc", true)) {
                    sender.gameMode = GameMode.CREATIVE
                    it.reply("&aGamemode is now creative.")
                    return@handler
                }

                if (label.equals("gms", true)) {
                    sender.gameMode = GameMode.SURVIVAL
                    it.reply("&aGamemode is now survival.")
                }
            }
            .registerAndBind(consumer, "gmc", "gms")

        Events.subscribe(PlayerCommandPreprocessEvent::class.java, EventPriority.LOWEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                val message = it.message
                if (!message.startsWith("/")) return@handler
                if (!message.contains(":")) return@handler
                if (player.hasPermission("server.coloncommands")) return@handler
                it.isCancelled = true
                player.sendMessage(Component.text("You cannot use colon commands.", NamedTextColor.RED))
            }
            .bindWith(consumer)

        Events.subscribe(PlayerAdvancementDoneEvent::class.java)
            .handler {
                it.message(null)
            }
            .bindWith(consumer)
    }
}