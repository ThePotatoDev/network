package gg.tater.oneblock.island.controllers

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
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
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.min

class OneBlockExperienceController : ExperienceService {

    private companion object {
        val SCHEDULER: ScheduledExecutorService = Executors.newScheduledThreadPool(5)

        const val BLOCK_MINING_PROGRESS_KEY = "block_mining_progress"

        const val MINE_GRASS_STAGE_PROGRESS = 1
        const val MINE_WOOD_STAGE_PROGRESS = 2
        const val CRAFT_PICKAXE_STAGE_PROGRESS = 3
        const val MINE_ALL_BLOCKS_STAGE_PROGRESS = 4
        const val INVESTIGATE_ROCKET_STAGE_PROGRESS = 5
        const val FIND_ROCKET_PARTS_STAGE_PROGRESS = 6
    }

    private lateinit var players: ExperiencePlayerService

    private val cache = CacheBuilder.newBuilder()
        .refreshAfterWrite(Duration.ofSeconds(10L))
        .build(CacheLoader.asyncReloading(object : CacheLoader<UUID, ExperiencePlayer>() {
            override fun load(key: UUID): ExperiencePlayer {
                return players.get(key).get()
            }
        }, SCHEDULER))

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

    override fun startExperience(player: Player) {
        val data = cache.get(player.uniqueId)
        data.setMeta(ExperiencePlayer.STAGE_PROGRESS, MINE_GRASS_STAGE_PROGRESS)
        players.save(data)

        sendExperienceMessage(
            Component.text(
                "Welcome to the OneBlock challenge! Everything starts with a single block. Mine the grass block to begin!",
                NamedTextColor.GREEN
            ),
            player
        )
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(ExperienceService::class.java, this)

        this.players = Services.provide(
            ExperiencePlayerService::class.java,
            consumer.bindModule(ExperiencePlayerController(GameModeType.ONEBLOCK))
        )

        Events.subscribe(NpcInteractEvent::class.java, EventPriority.HIGHEST)
            .handler {
                val npc = it.npc
                val action = it.interactionType
                if (action != ActionTrigger.RIGHT_CLICK) return@handler

                val location = npc.data.location
                if (WrappedPosition(location) != OneBlockIsland.SPACE_SHIP_NPC_LOCATION) return@handler

                val clicker = it.player

                val player = cache.get(clicker.uniqueId)

                if (!player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, INVESTIGATE_ROCKET_STAGE_PROGRESS)) {
                    clicker.sendMessage(
                        Component.text(
                            "Hmm... I don't think I have a task for you yet.",
                            NamedTextColor.GREEN
                        )
                    )
                    return@handler
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
            .bindWith(consumer)

        Events.subscribe(CraftItemEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.whoClicked.type == EntityType.PLAYER }
            .handler {
                val result = it.recipe.result
                val clicker = it.whoClicked as Player

                if (result.type != Material.WOODEN_PICKAXE) return@handler

                val player = cache.get(clicker.uniqueId)

                if (!player.hasMetaEqualTo(
                        ExperiencePlayer.STAGE_PROGRESS,
                        CRAFT_PICKAXE_STAGE_PROGRESS
                    )
                ) return@handler

                sendExperienceMessage(
                    Component.text("Now, mine 10 blocks to gather resources!", NamedTextColor.GREEN), clicker
                )

                player.setMeta(ExperiencePlayer.STAGE_PROGRESS, MINE_ALL_BLOCKS_STAGE_PROGRESS)
                player.setMeta(BLOCK_MINING_PROGRESS_KEY, 0)
                players.save(player)
            }
            .bindWith(consumer)

        Events.subscribe(OneBlockMineEvent::class.java, EventPriority.HIGH)
            .handler {
                val miner = it.player
                val block = it.block
                val type = block.type

                if (!it.island.ftue) return@handler
                val player = cache.get(miner.uniqueId)

                it.handled = true

                // If they are supposed to craft a pickaxe, don't allow them to break block
                if (player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, CRAFT_PICKAXE_STAGE_PROGRESS)) {
                    it.isCancelled = true
                    return@handler
                }

                if (player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, FIND_ROCKET_PARTS_STAGE_PROGRESS)) {

                    return@handler
                }

                if (player.hasMetaEqualTo(ExperiencePlayer.STAGE_PROGRESS, MINE_ALL_BLOCKS_STAGE_PROGRESS)) {
                    val amountMined = player.getMetaValue(BLOCK_MINING_PROGRESS_KEY)!!.toInt()

                    // If they are on block #10, set a chest block
                    // that contains a rocket ship part
                    if (amountMined + 1 == 10) {
                        it.nextMaterialType = Material.CHEST

                        miner.inventory.addItem(ItemStackBuilder.of(Material.PAPER)
                            .name("&eRocket Ship Engine")
                            .transformMeta { meta ->
                                meta.setCustomModelData(7000)
                            }
                            .build())

                        sendExperienceMessage(
                            Component.text(
                                "Huh? This looks important... maybe you should investigate that crashed rocket!",
                                NamedTextColor.GREEN
                            ), miner
                        )

                        player.setMeta(ExperiencePlayer.STAGE_PROGRESS, INVESTIGATE_ROCKET_STAGE_PROGRESS)
                        players.save(player)
                        cache.refresh(player.uuid)

                        return@handler
                    }

                    it.nextMaterialType =
                        cycleMaterials[ThreadLocalRandom.current().nextInt(0, cycleMaterials.size)]

                    player.setMeta(BLOCK_MINING_PROGRESS_KEY, amountMined + 1)
                    players.save(player)
                    cache.refresh(player.uuid)

                    return@handler
                }

                if (type == Material.GRASS_BLOCK && player.hasMetaEqualTo(
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
                    cache.refresh(player.uuid)

                    return@handler
                }

                if (type == Material.OAK_LOG && player.hasMetaEqualTo(
                        ExperiencePlayer.STAGE_PROGRESS,
                        MINE_WOOD_STAGE_PROGRESS
                    )
                ) {
                    it.nextMaterialType = Material.CRAFTING_TABLE

                    player.setMeta(ExperiencePlayer.STAGE_PROGRESS, CRAFT_PICKAXE_STAGE_PROGRESS)
                    players.save(player)
                    cache.refresh(player.uuid)

                    sendExperienceMessage(
                        Component.text(
                            "Now, craft a wooden pickaxe to mine faster!",
                            NamedTextColor.GREEN
                        ), miner
                    )
                }
            }
            .bindWith(consumer)
    }
}