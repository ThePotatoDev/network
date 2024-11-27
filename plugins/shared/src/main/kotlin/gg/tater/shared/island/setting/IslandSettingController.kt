package gg.tater.shared.island.setting

import gg.tater.shared.island.IslandService
import gg.tater.shared.island.setting.model.IslandSettingHandler
import gg.tater.shared.island.setting.model.handlers.*
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

class IslandSettingController(val service: IslandService) : TerminableModule {

    private val handlers: MutableSet<IslandSettingHandler> = mutableSetOf()

    override fun setup(consumer: TerminableConsumer) {
        handlers.add(SpawnAnimalsSettingHandler(service))
        handlers.add(SpawnMobsSettingHandler(service))
        handlers.add(PvPSettingHandler(service))
        handlers.add(FireSpreadSettingHandler(service))
        handlers.add(ExplosionsSettingHandler(service))
        handlers.add(RainingSettingHandler(service))

        for (handler in handlers) {
            consumer.bindModule(handler)
        }
    }
}