package gg.tater.oneblock.experience

import gg.tater.core.island.experience.ExperienceService
import gg.tater.core.island.experience.player.ExperiencePlayer
import gg.tater.core.island.experience.player.ExperiencePlayerController
import gg.tater.core.island.experience.player.ExperiencePlayerService
import gg.tater.core.server.model.GameModeType
import gg.tater.oneblock.event.OneBlockMineEvent
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.terminable.TerminableConsumer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.CraftItemEvent
import java.util.concurrent.CompletionStage
import java.util.concurrent.ThreadLocalRandom

class OneBlockExperienceController : ExperienceService {

    private companion object {
        const val BLOCK_MINING_PROGRESS_KEY = "block_mining_progress"
    }

    private val cycleMaterials: List<Material> =
        listOf(
            Material.STONE,
            Material.DIRT,
            Material.OAK_LOG,
            Material.COAL_ORE,
            Material.REDSTONE_ORE,
            Material.DIAMOND_ORE,
            Material.GRASS_BLOCK
        )

    private lateinit var players: ExperiencePlayerService

    override fun startExperience(player: Player): CompletionStage<Void> {
        return players.get(player.uniqueId).thenAccept { data ->
            data.setMeta(ExperiencePlayer.STAGE_PROGRESS, 1)
            players.save(data)
        }
    }

    override fun setup(consumer: TerminableConsumer) {
        this.players = Services.provide(
            ExperiencePlayerService::class.java,
            consumer.bindModule(ExperiencePlayerController(GameModeType.ONEBLOCK))
        )

        val players = Services.load(ExperiencePlayerService::class.java)

        Events.subscribe(CraftItemEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.whoClicked.type == EntityType.PLAYER }
            .handler {
                val result = it.recipe.result
                val clicker = it.whoClicked as Player

                if (result.type != Material.WOODEN_PICKAXE) return@handler

                players.get(clicker.uniqueId).thenAccept { player ->
                    if (!player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, 2)) return@thenAccept

                    sendExperienceMessage(
                        Component.text("Now, mine 10 blocks to gather resources!", NamedTextColor.GREEN), clicker
                    )

                    player.setMeta(ExperiencePlayer.STAGE_PROGRESS, 3)
                    player.setMeta(BLOCK_MINING_PROGRESS_KEY, 0)
                    players.save(player)
                }
            }
            .bindWith(consumer)

        Events.subscribe(OneBlockMineEvent::class.java)
            .handler {
                val miner = it.player
                val block = it.block

                players.get(miner.uniqueId).thenAcceptAsync { player ->

                    if (player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, 3)) {
                        val amountMined = player.getMetaValue(BLOCK_MINING_PROGRESS_KEY)!!.toInt()

                        // If they are on block #10, set a chest block
                        // that contains a rocket ship part
                        if (amountMined + 1 == 10) {

                            Schedulers.sync().runLater({
                                block.type = Material.CHEST
                                val chest = block as Chest

                                chest.inventory.setItem(
                                    13, ItemStackBuilder.of(Material.STONE)
                                        .name("Rocket Ship Item")
                                        .build()
                                )
                            }, 1L)

                            sendExperienceMessage(
                                Component.text(
                                    "Huh? This looks important... maybe you should investigate that crashed rocket!",
                                    NamedTextColor.GREEN
                                ), miner
                            )

                            player.setMeta(ExperiencePlayer.STAGE_PROGRESS, 4)
                            players.save(player)

                            return@thenAcceptAsync
                        }

                        it.nextMaterialType =
                            cycleMaterials[ThreadLocalRandom.current().nextInt(0, cycleMaterials.size)]

                        player.setMeta(BLOCK_MINING_PROGRESS_KEY, amountMined + 1)
                        players.save(player)
                        return@thenAcceptAsync
                    }

                    if (block.type == Material.GRASS_BLOCK && player.hasMetaEqualTo(
                            ExperiencePlayer.STAGE_PROGRESS,
                            1
                        )
                    ) {
                        it.nextMaterialType = Material.OAK_LOG

                        sendExperienceMessage(
                            Component.text(
                                "Great! Some blocks give different materials. Mine the wood next.",
                                NamedTextColor.GREEN
                            ), miner
                        )

                        player.setMeta(ExperiencePlayer.STAGE_PROGRESS, 2)
                        players.save(player)

                        return@thenAcceptAsync
                    }

                    if (block.type == Material.OAK_LOG && player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, 2)) {
                        it.nextMaterialType = Material.CRAFTING_TABLE

                        sendExperienceMessage(
                            Component.text(
                                "Now, craft a wooden pickaxe to mine faster!",
                                NamedTextColor.GREEN
                            ), miner
                        )
                    }
                }
            }
            .bindWith(consumer)
    }
}