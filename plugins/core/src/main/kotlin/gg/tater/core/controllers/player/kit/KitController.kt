package gg.tater.core.controllers.player.kit

import gg.tater.shared.redis.Redis
import gg.tater.shared.player.kit.KitDataModel
import gg.tater.shared.player.kit.KitPlayerDataModel
import gg.tater.shared.player.kit.gui.KitGui
import me.lucko.helper.Commands
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class KitController(private val redis: Redis) : TerminableModule {

    companion object {
        val KITS: Set<KitDataModel> = setOf(
            KitDataModel(
                10,
                "Starter",
                "&fStarter Kit",
                "",
                3600000,
                listOf(ItemStack(Material.STONE_PICKAXE)),
                ItemStackBuilder.of(Material.STONE_PICKAXE)
                    .build(),
                true
            )
        )
    }

    override fun setup(consumer: TerminableConsumer) {
        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
                redis.kits().computeIfAbsentAsync(sender.uniqueId) { KitPlayerDataModel() }
                    .thenAccept { data -> KitGui(sender, KITS, data, redis).open() }
            }
            .registerAndBind(consumer, "kits")
    }
}