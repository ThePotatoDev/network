package gg.tater.core.controllers.server.hub

import gg.tater.shared.annotation.Controller
import me.lucko.helper.Events
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.event.player.PlayerJoinEvent

@Controller("hub-controller")
class HubController : TerminableModule {

    private companion object {

    }

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(PlayerJoinEvent::class.java)
            .handler {
                val player = it.player
            }
            .bindWith(consumer)
    }
}