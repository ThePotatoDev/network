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
import org.bukkit.Material

class OneBlockExperienceController : TerminableModule {

    private companion object {
        const val STAGE_ONE_PROGRESS = "stage_one_progress"
    }

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
                            val miner = it.player
                            val island = it.island
                            val block = it.block

                            if (block.type != Material.GRASS_BLOCK && block.type != Material.OAK_LOG) return@handler

                            players.get(miner.uniqueId).thenAccept { player ->
                                if (block.type == Material.GRASS_BLOCK) {
                                    // If player has already completed stage 1, ignore
                                    if (player.hasMetaEqualTo(STAGE_ONE_PROGRESS, 1)) return@thenAccept
                                    it.nextMaterialType = Material.OAK_LOG



                                    return@thenAccept
                                }

                                // If player is attemtping stage two but hasn't completed stage 1
                                if (block.type == Material.OAK_LOG && !player.hasMetaEqualTo(STAGE_ONE_PROGRESS, 2)) {
                                    return@thenAccept
                                }
                            }
                        }
                        .bindWith(consumer)
                }
            )
        )
    }
}