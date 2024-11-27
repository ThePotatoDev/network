package gg.tater.core.controllers.player

import com.google.common.collect.ArrayListMultimap
import gg.tater.shared.redis.Redis
import gg.tater.shared.player.progression.PlayerProgressDataModel
import gg.tater.shared.player.progression.skill.SkillLandingGui
import gg.tater.shared.player.progression.skill.model.SkillReward
import gg.tater.shared.player.progression.skill.model.SkillType
import gg.tater.shared.player.progression.task.TaskHandler
import gg.tater.shared.player.progression.task.handler.MineBlockTaskHandler
import me.lucko.helper.Commands
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

class ProgressionController(private val redis: Redis) : TerminableModule {

    private val rewards: ArrayListMultimap<SkillType, SkillReward> = ArrayListMultimap.create()

    private val tasks: MutableSet<TaskHandler> = mutableSetOf()

    override fun setup(consumer: TerminableConsumer) {
        tasks.add(MineBlockTaskHandler(redis))

        for (task in tasks) {
            consumer.bindModule(task)
        }

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
                redis.progressions()
                    .computeIfAbsentAsync(sender.uniqueId) { _ -> PlayerProgressDataModel(sender.uniqueId) }
                    .thenAccept { data ->
                        SkillLandingGui(sender, data).open()
                    }
            }
            .registerAndBind(consumer, "skills", "skill")
    }
}