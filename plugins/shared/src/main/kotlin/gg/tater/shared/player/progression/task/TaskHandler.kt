package gg.tater.shared.player.progression.task

import gg.tater.shared.redis.Redis
import me.lucko.helper.terminable.module.TerminableModule

abstract class TaskHandler(val name: String, val type: TaskType, val redis: Redis) : TerminableModule