package gg.tater.oneblock.experience

import gg.tater.core.island.experience.ExperienceController
import gg.tater.core.island.experience.player.ExperiencePlayer
import gg.tater.core.island.experience.player.ExperiencePlayerService
import gg.tater.core.island.experience.stage.ExperienceStage
import gg.tater.core.server.model.GameModeType
import gg.tater.oneblock.event.OneBlockMineEvent
import gg.tater.oneblock.island.OneBlockIsland
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.CraftItemEvent

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
                ), {
                    Events.subscribe(CraftItemEvent::class.java, EventPriority.HIGHEST)
                        .filter(EventFilters.ignoreCancelled())
                        .handler {
                            val result = it.recipe.result
                            val uuid = it.whoClicked.uniqueId

                            if (result.type != Material.WOODEN_PICKAXE) return@handler

                            players.get(uuid).thenAccept { player ->
                                if (!player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, 2)) return@thenAccept

                                player.setMeta(ExperiencePlayer.STAGE_PROGRESS, 3)

                            }
                        }
                        .bindWith(consumer)

                    Events.subscribe(OneBlockMineEvent::class.java)
                        .handler {
                            val miner = it.player
                            val island = it.island
                            val block = it.block

                            if (block.type != Material.GRASS_BLOCK && block.type != Material.OAK_LOG) return@handler

                            players.get(miner.uniqueId).thenAccept { player ->
                                if (block.type == Material.GRASS_BLOCK) {
                                    // If player has already completed stage 1, ignore
                                    if (player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, 1)) return@thenAccept
                                    it.nextMaterialType = Material.OAK_LOG

                                    arrayOf(
                                        Component.empty(),
                                        Component.text(
                                            "Great! Some blocks give different materials. Mine the wood next.",
                                            NamedTextColor.GREEN
                                        ),
                                        Component.empty()
                                    ).forEach { message -> miner.sendMessage(message) }

                                    player.setMeta(ExperiencePlayer.STAGE_PROGRESS, 2)
                                    players.save(player)

                                    return@thenAccept
                                }

                                if (block.type == Material.OAK_LOG) {
                                    // If player is attemtping stage two but hasn't completed stage 1, cancel
                                    if (!player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, 2)) return@thenAccept

                                    it.nextMaterialType = Material.CRAFTING_TABLE

                                    arrayOf(
                                        Component.empty(),
                                        Component.text(
                                            "Now, craft a wooden pickaxe to mine faster!",
                                            NamedTextColor.GREEN
                                        ),
                                        Component.empty()
                                    ).forEach { message -> miner.sendMessage(message) }
                                }
                            }
                        }
                        .bindWith(consumer)
                }
            )
        )
    }
}