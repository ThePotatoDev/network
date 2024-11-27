package gg.tater.shared.player.progression.task.handler

import gg.tater.shared.player.progression.task.TaskHandler
import gg.tater.shared.player.progression.task.TaskType
import gg.tater.shared.redis.Redis
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent

class CutWoodTaskHandler(redis: Redis) : TaskHandler("Cut Wood", TaskType.CUT_WOOD, redis) {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(BlockBreakEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
            }
            .bindWith(consumer)
    }
}