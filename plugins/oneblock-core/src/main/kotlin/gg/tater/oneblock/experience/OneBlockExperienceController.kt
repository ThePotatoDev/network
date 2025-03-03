package gg.tater.oneblock.experience

import gg.tater.core.island.experience.ExperienceController
import gg.tater.core.island.experience.player.ExperiencePlayerService
import gg.tater.core.island.experience.stage.ExperienceStage
import gg.tater.core.server.model.GameModeType
import gg.tater.oneblock.event.OneBlockMineEvent
import gg.tater.oneblock.island.OneBlockIsland
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class OneBlockExperienceController : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        val controller = ExperienceController<OneBlockIsland>(GameModeType.ONEBLOCK)
        val players = Services.load(ExperiencePlayerService::class.java)

        controller.addStage(
            ExperienceStage(
                1,
                arrayOf(
                    Component.empty(),
                    Component.text(
                        "Welcome to the OneBlock challenge! Everything starts with a single block. Mine the grass block to begin!",
                        NamedTextColor.GREEN
                    ),
                    Component.empty()
                ),
                arrayOf(
                    Component.empty(),
                    Component.text(
                        "Great! Some blocks give different materials. Mine the wood next.",
                        NamedTextColor.GREEN
                    ),
                    Component.empty()
                ), {
                    Events.subscribe(OneBlockMineEvent::class.java)
                        .handler {
                            val player = it.player
                            val island = it.island
                        }
                        .bindWith(consumer)
                }
            )
        )
    }
}