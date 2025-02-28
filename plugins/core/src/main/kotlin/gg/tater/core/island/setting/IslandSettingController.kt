package gg.tater.core.island.setting

import gg.tater.core.island.setting.model.IslandSettingHandler
import gg.tater.core.island.setting.model.handlers.*
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

class IslandSettingController : TerminableModule {

    private val handlers: MutableSet<IslandSettingHandler> = mutableSetOf()

    override fun setup(consumer: TerminableConsumer) {
        handlers.add(SpawnAnimalsSettingHandler())
        handlers.add(SpawnMobsSettingHandler())
        handlers.add(PvPSettingHandler())
        handlers.add(FireSpreadSettingHandler())
        handlers.add(ExplosionsSettingHandler())
        handlers.add(RainingSettingHandler())

        for (handler in handlers) {
            consumer.bindModule(handler)
        }
    }
}