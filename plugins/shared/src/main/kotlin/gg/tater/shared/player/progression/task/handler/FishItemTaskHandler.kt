package gg.tater.shared.player.progression.task.handler

import gg.tater.shared.player.progression.task.TaskHandler
import gg.tater.shared.player.progression.task.TaskType
import gg.tater.shared.redis.Redis
import me.lucko.helper.terminable.TerminableConsumer

class FishItemTaskHandler(redis: Redis) : TaskHandler("fish_item", TaskType.FISH_ITEM, redis) {

    override fun setup(consumer: TerminableConsumer) {
        TODO("Not yet implemented")
    }
}