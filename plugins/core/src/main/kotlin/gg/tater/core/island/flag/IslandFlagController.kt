package gg.tater.core.island.flag

import gg.tater.core.island.flag.model.IslandFlagHandler
import gg.tater.core.island.flag.model.handlers.*
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule

class IslandFlagController : TerminableModule {

    private val flags: MutableSet<IslandFlagHandler> = mutableSetOf()

    override fun setup(consumer: TerminableConsumer) {
        flags.add(BreakBlockFlagHandler())
        flags.add(PlaceBlockFlagHandler())
        flags.add(DamageAnimalsFlagHandler())
        flags.add(DamageMobFlagHandler())
        flags.add(DropItemsFlagHandler())
        flags.add(InteractBlocksFlagHandler())
        flags.add(OpenContainersFlagHandler())
        flags.add(PickupItemsFlagHandler())
        flags.add(UseButtonsFlagHandler())
        flags.add(UseDoorFlagHandler())
        flags.add(UseEnderpearlsFlagHandler())
        flags.add(UseLeversFlagHandler())
        flags.add(UsePressurePlatesFlagHandler())
        flags.add(VillagerTradingFlagHandler())
        flags.add(BreakSpawnersFlagHandler())

        for (flag in flags) {
            consumer.bindModule(flag)
            println("Registered flag handler: ${flag.type().name}")
        }
    }
}