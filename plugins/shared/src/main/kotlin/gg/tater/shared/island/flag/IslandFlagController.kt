package gg.tater.shared.island.flag

import gg.tater.shared.island.flag.model.IslandFlagHandler
import gg.tater.shared.island.flag.model.handlers.*
import gg.tater.shared.island.IslandService
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

class IslandFlagController(private val service: IslandService) : TerminableModule {

    private val flags: MutableSet<IslandFlagHandler> = mutableSetOf()

    override fun setup(consumer: TerminableConsumer) {
        flags.add(BreakBlockFlagHandler(service))
        flags.add(PlaceBlockFlagHandler(service))
        flags.add(DamageAnimalsFlagHandler(service))
        flags.add(DamageMobFlagHandler(service))
        flags.add(DropItemsFlagHandler(service))
        flags.add(InteractBlocksFlagHandler(service))
        flags.add(OpenContainersFlagHandler(service))
        flags.add(PickupItemsFlagHandler(service))
        flags.add(UseButtonsFlagHandler(service))
        flags.add(UseDoorFlagHandler(service))
        flags.add(UseEnderpearlsFlagHandler(service))
        flags.add(UseLeversFlagHandler(service))
        flags.add(UsePressurePlatesFlagHandler(service))
        flags.add(VillagerTradingFlagHandler(service))
        flags.add(BreakSpawnersFlagHandler(service))

        for (flag in flags) {
            consumer.bindModule(flag)
            println("Registered flag handler: ${flag.type().name}")
        }
    }
}