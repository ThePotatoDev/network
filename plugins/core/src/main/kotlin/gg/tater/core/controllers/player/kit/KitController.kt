package gg.tater.core.controllers.player.kit

import gg.tater.shared.annotation.Controller
import gg.tater.shared.player.kit.KitDataModel
import gg.tater.shared.player.kit.KitPlayerDataModel
import gg.tater.shared.player.kit.KitService
import gg.tater.shared.player.kit.gui.KitGui
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.redisson.api.RFuture
import java.util.*

@Controller(id = "kit-controller")
class KitController : KitService {

    companion object {
        private const val KITS_MAP_NAME = "kits"

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

    private val redis = Services.load(Redis::class.java)

    override fun compute(uuid: UUID): RFuture<KitPlayerDataModel> {
        return redis.client.getMap<UUID, KitPlayerDataModel>(KITS_MAP_NAME)
            .computeIfAbsentAsync(uuid) {
                KitPlayerDataModel()
            }
    }

    override fun save(uuid: UUID, data: KitPlayerDataModel): RFuture<Boolean> {
        return redis.client.getMap<UUID, KitPlayerDataModel>(KITS_MAP_NAME)
            .fastPutAsync(uuid, data)
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(KitService::class.java, this)

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
                compute(sender.uniqueId).thenAccept { player -> KitGui(sender, KITS, player, redis).open() }
            }
            .registerAndBind(consumer, "kits")
    }
}