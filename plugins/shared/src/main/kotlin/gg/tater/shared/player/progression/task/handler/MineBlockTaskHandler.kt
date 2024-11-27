package gg.tater.shared.player.progression.task.handler

import gg.tater.shared.player.progression.task.TaskHandler
import gg.tater.shared.player.progression.task.TaskType
import gg.tater.shared.redis.Redis
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent

class MineBlockTaskHandler(redis: Redis) : TaskHandler("mine_block", TaskType.MINE_BLOCK, redis) {

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(BlockBreakEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                val player = it.player
                val block = it.block

                redis.progressions()
                    .getAsync(player.uniqueId)
                    .thenAccept { data ->
                        if (data == null) return@thenAccept


                    }
            }
            .bindWith(consumer)
    }
}