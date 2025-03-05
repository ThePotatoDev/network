package gg.tater.oneblock.island.phase

import gg.tater.oneblock.event.OneBlockMineEvent
import gg.tater.oneblock.island.phase.model.OneBlockPhase
import gg.tater.oneblock.island.phase.model.OneBlockPhaseSerivce
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Material
import org.bukkit.event.EventPriority

class OneBlockPhaseController : OneBlockPhaseSerivce {

    private val phases: Map<Int, OneBlockPhase> = mapOf(
        0 to OneBlockPhase(
            0,
            "Underground",
            50,
            ItemStackBuilder.of(Material.STONE),
            listOf(Pair(Material.STONE, 10)),
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

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(OneBlockPhaseSerivce::class.java, this)

        Events.subscribe(OneBlockMineEvent::class.java, EventPriority.HIGHEST)
            .handler {
                val island = it.island
                if (it.handled) return@handler
            }
            .bindWith(consumer)
    }
}