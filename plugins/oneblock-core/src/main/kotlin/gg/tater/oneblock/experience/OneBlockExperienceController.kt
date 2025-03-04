package gg.tater.oneblock.experience

import de.oliver.fancynpcs.api.actions.ActionTrigger
import de.oliver.fancynpcs.api.events.NpcInteractEvent
import gg.tater.core.island.experience.ExperienceService
import gg.tater.core.island.experience.player.ExperiencePlayer
import gg.tater.core.island.experience.player.ExperiencePlayerController
import gg.tater.core.island.experience.player.ExperiencePlayerService
import gg.tater.core.position.WrappedPosition
import gg.tater.core.server.model.GameModeType
import gg.tater.oneblock.event.OneBlockMineEvent
import gg.tater.oneblock.island.OneBlockIsland
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
import org.bukkit.event.player.PlayerInteractEntityEvent
import java.util.concurrent.CompletionStage
import java.util.concurrent.ThreadLocalRandom

class OneBlockExperienceController : ExperienceService {

    private companion object {
        const val BLOCK_MINING_PROGRESS_KEY = "block_mining_progress"

        const val MINE_GRASS_STAGE_PROGRESS = 1
        const val MINE_WOOD_STAGE_PROGRESS = 2
        const val MINE_ALL_BLOCKS_STAGE_PROGRESS = 3
        const val INVESTIGATE_ROCKET_STAGE_PROGRESS = 4
        const val FIND_ROCKET_PARTS_STAGE_PROGRESS = 5
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
            data.setMeta(ExperiencePlayer.STAGE_PROGRESS, MINE_GRASS_STAGE_PROGRESS)
            players.save(data)
        }
    }

    override fun setup(consumer: TerminableConsumer) {
        this.players = Services.provide(
            ExperiencePlayerService::class.java,
            consumer.bindModule(ExperiencePlayerController(GameModeType.ONEBLOCK))
        )

        val players = Services.load(ExperiencePlayerService::class.java)

        Events.subscribe(NpcInteractEvent::class.java, EventPriority.HIGHEST)
            .handler {
                val npc = it.npc
                val action = it.interactionType
                if (action != ActionTrigger.RIGHT_CLICK) return@handler

                val location = npc.data.location
                if (WrappedPosition(location) != OneBlockIsland.SPACE_SHIP_NPC_LOCATION) return@handler

                val clicker = it.player

                players.get(clicker.uniqueId).thenAcceptAsync { player ->
                    if (!player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, INVESTIGATE_ROCKET_STAGE_PROGRESS)) {
                        clicker.sendMessage(
                            Component.text(
                                "Hmm... I don't think I have a task for you yet.",
                                NamedTextColor.GREEN
                            )
                        )
                        return@thenAcceptAsync
                    }

                    arrayOf(
                        Component.empty(),
                        Component.text(
                            "Ah! You found a ship part! I crash-landed here and need your help to fix my rocket.",
                            NamedTextColor.GREEN
                        ),
                        Component.text(
                            "If you can recover all my ship parts, I can take you to distant planets filled with rare resources!",
                            NamedTextColor.GREEN
                        ),
                        Component.text(
                            "The OneBlock contains everything you need, but you'll have to dig and search for the remaining parts.",
                            NamedTextColor.GREEN
                        ),
                        Component.text(
                            "The OneBlock changes over time! The more you mine, the faster you rank up and unlock a new phase!",
                            NamedTextColor.GREEN
                        ),
                        Component.empty()
                    ).forEach { msg -> clicker.sendMessage(msg) }

                    player.setMeta(ExperiencePlayer.STAGE_PROGRESS, FIND_ROCKET_PARTS_STAGE_PROGRESS)
                    players.save(player)
                }
            }
            .bindWith(consumer)

        Events.subscribe(CraftItemEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.whoClicked.type == EntityType.PLAYER }
            .handler {
                val result = it.recipe.result
                val clicker = it.whoClicked as Player

                if (result.type != Material.WOODEN_PICKAXE) return@handler

                players.get(clicker.uniqueId).thenAccept { player ->
                    if (!player.hasMetaEqualTo(
                            ExperiencePlayer.STAGE_PROGRESS,
                            MINE_WOOD_STAGE_PROGRESS
                        )
                    ) return@thenAccept

                    sendExperienceMessage(
                        Component.text("Now, mine 10 blocks to gather resources!", NamedTextColor.GREEN), clicker
                    )

                    player.setMeta(ExperiencePlayer.STAGE_PROGRESS, MINE_ALL_BLOCKS_STAGE_PROGRESS)
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

                    if (player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, FIND_ROCKET_PARTS_STAGE_PROGRESS)) {


                        return@thenAcceptAsync
                    }

                    if (player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, MINE_ALL_BLOCKS_STAGE_PROGRESS)) {
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

                            player.setMeta(ExperiencePlayer.STAGE_PROGRESS, INVESTIGATE_ROCKET_STAGE_PROGRESS)
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
                            MINE_GRASS_STAGE_PROGRESS
                        )
                    ) {
                        it.nextMaterialType = Material.OAK_LOG

                        sendExperienceMessage(
                            Component.text(
                                "Great! Some blocks give different materials. Mine the wood next.",
                                NamedTextColor.GREEN
                            ), miner
                        )

                        player.setMeta(ExperiencePlayer.STAGE_PROGRESS, MINE_WOOD_STAGE_PROGRESS)
                        players.save(player)

                        return@thenAcceptAsync
                    }

                    if (block.type == Material.OAK_LOG && player.hasMetaEqualTo(
                            ExperiencePlayer.STAGE_PROGRESS,
                            MINE_WOOD_STAGE_PROGRESS
                        )
                    ) {
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