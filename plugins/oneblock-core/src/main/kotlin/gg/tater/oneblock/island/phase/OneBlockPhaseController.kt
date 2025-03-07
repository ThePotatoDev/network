package gg.tater.oneblock.island.phase

import gg.tater.core.getRandomWeightedItem
import gg.tater.oneblock.event.OneBlockMineEvent
import gg.tater.oneblock.event.OneBlockPhaseCompleteEvent
import gg.tater.oneblock.island.controllers.OneBlockIslandService
import gg.tater.oneblock.island.phase.model.OneBlockPhase
import gg.tater.oneblock.island.phase.model.OneBlockPhaseSerivce
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority

class OneBlockPhaseController : OneBlockPhaseSerivce {

    private val islands = Services.load(OneBlockIslandService::class.java)

    private val phases: Map<Int, OneBlockPhase> = mapOf(
        0 to OneBlockPhase(
            0,
            "Underground",
            100,
            ItemStackBuilder.of(Material.STONE),
            listOf(Pair(Material.STONE, 10), Pair(Material.GRASS_BLOCK, 10), Pair(Material.MOSS_BLOCK, 10)),
            mapOf(
                50 to listOf("give {player} paper[minecraft:custom_model_data=7002]"),
                100 to listOf()
            )
        )
    )

    override fun all(): Collection<OneBlockPhase> {
        return phases.values
    }

    override fun default(): OneBlockPhase {
        return phases[0]!!
    }

    override fun getById(id: Int): OneBlockPhase {
        return phases[id]!!
    }

    override fun getNextPhase(current: OneBlockPhase): OneBlockPhase? {
        return phases[current.id + 1]
    }

    override fun getProgressiveRewards(phase: OneBlockPhase, count: Int): List<String>? {
        val found = phases[phase.id] ?: return null
        return found.progressiveRewardCommands[count]
    }

    override fun dispatchProgressiveRewards(player: Player, rewards: List<String>) {
        Schedulers.sync().run {
            for (reward in rewards) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("{player}", player.name))
            }
        }
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(OneBlockPhaseSerivce::class.java, this)

        Events.subscribe(OneBlockPhaseCompleteEvent::class.java)
            .handler {
                val island = it.island
                val phase = it.phase
                println("Completed phase ${phase.id}")
            }
            .bindWith(consumer)

        Events.subscribe(OneBlockMineEvent::class.java, EventPriority.HIGHEST)
            .handler {
                val island = it.island
                val player = it.player

                // If the OneBlock mining event is handled elsewhere explicitly, don't handle phase mining
                if (it.handled) return@handler

                val currentPhase = island.getCurrentPhase()
                val randomNextBlockMaterial = getRandomWeightedItem(currentPhase.blocks)
                it.nextMaterialType = randomNextBlockMaterial

                val incremented = island.incrementBlocksMined()

                val rewards = getProgressiveRewards(currentPhase, incremented)
                if (rewards != null) {
                    dispatchProgressiveRewards(player, rewards)
                }

                if (island.shouldCompletePhase(currentPhase)) {
                    island.completePhase(currentPhase)
                    Events.callSync(OneBlockPhaseCompleteEvent(island, currentPhase))

                    val nextPhase = getNextPhase(currentPhase)
                    if (nextPhase != null) {
                        island.currentPhase = nextPhase.id
                    }
                }

                islands.save(island)
            }
            .bindWith(consumer)
    }
}