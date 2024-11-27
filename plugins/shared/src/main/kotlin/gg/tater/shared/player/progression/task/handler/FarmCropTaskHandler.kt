package gg.tater.shared.player.progression.task.handler

import gg.tater.shared.player.progression.task.TaskHandler
import gg.tater.shared.player.progression.task.TaskType
import gg.tater.shared.redis.Redis
import me.lucko.helper.terminable.TerminableConsumer

class FarmCropTaskHandler(redis: Redis) : TaskHandler("farm_crop", TaskType.FARM_CROP, redis) {

    override fun setup(consumer: TerminableConsumer) {

    }
}