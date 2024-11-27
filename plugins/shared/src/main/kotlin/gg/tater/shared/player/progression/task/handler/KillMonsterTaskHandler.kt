package gg.tater.shared.player.progression.task.handler

import gg.tater.shared.player.progression.task.TaskHandler
import gg.tater.shared.player.progression.task.TaskType
import gg.tater.shared.redis.Redis
import me.lucko.helper.terminable.TerminableConsumer

class KillMonsterTaskHandler(redis: Redis) : TaskHandler("kill_monster", TaskType.KILL_MONSTER, redis) {

    override fun setup(consumer: TerminableConsumer) {

    }
}