package gg.tater.core.player.vault

import gg.tater.core.ARROW_TEXT
import gg.tater.core.annotation.Controller
import gg.tater.core.player.vault.gui.VaultGuiItem
import gg.tater.core.player.vault.gui.VaultItemGui
import gg.tater.core.player.vault.gui.VaultSelectionGui
import gg.tater.core.redis.Redis
import gg.tater.core.server.model.GameModeType
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryCloseEvent
import org.redisson.api.RFuture
import java.util.*

@Controller(
    id = "player-vault-controller"
)
class VaultController(mode: GameModeType) : VaultService {

    private val mapName = "${mode.id}_vaults"

    private val redis = Services.load(Redis::class.java)

    override fun setup(consumer: TerminableConsumer) {
        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

                compute(sender.uniqueId).thenAccept { vault ->
                    Schedulers.sync().run {
                        if (it.args().isEmpty()) {
                            val amount = vault.amount
                            val items: MutableList<VaultGuiItem> = mutableListOf()

                            for (i in 0 until amount) {
                                val id = i + 1

                                items.add(
                                    VaultGuiItem(
                                        id, ItemStackBuilder.of(Material.ENDER_CHEST)
                                            .name("&3&lVault #$id")
                                            .lore(
                                                " ",
                                                "$ARROW_TEXT &7Click to open this vault",
                                                " "
                                            )
                                            .build { VaultItemGui(sender, i, vault.getVaultItems(i)).open() })
                                )
                            }

                            VaultSelectionGui(sender, items).open()
                            return@run
                        }

                        val index = it.arg(0).parseOrFail(String::class.java).toInt() - 1 // Grab the index
                        if (index == -1) {
                            it.reply("&cPlease specify a valid vault number.")
                            return@run
                        }

                        if (index + 1 > vault.amount) {
                            it.reply("&cYou do not have access to that vault!")
                            return@run
                        }

                        VaultItemGui(sender, index, vault.getVaultItems(index)).open()
                    }
                }
            }
            .registerAndBind(consumer, "playervault", "pv")

        Events.subscribe(InventoryCloseEvent::class.java, EventPriority.HIGHEST)
            .handler {
                val player = it.player as Player
                val inventory = it.inventory
                if (inventory.holder !is VaultItemGui) return@handler

                val gui = inventory.holder as VaultItemGui
                val id = gui.id

                get(player.uniqueId).thenAccept { vault ->
                    vault.setVaultItems(id, inventory.contents)
                    save(player.uniqueId, vault)
                }

                player.performCommand("pv")
            }
            .bindWith(consumer)
    }

    override fun compute(uuid: UUID): RFuture<VaultDataModel> {
        return redis.client.getMap<UUID, VaultDataModel>(mapName)
            .computeIfAbsentAsync(uuid) { VaultDataModel(uuid) }
    }

    override fun save(uuid: UUID, vault: VaultDataModel): RFuture<VaultDataModel> {
        return redis.client.getMap<UUID, VaultDataModel>(mapName)
            .putAsync(uuid, vault)
    }

    override fun get(uuid: UUID): RFuture<VaultDataModel> {
        return redis.client.getMap<UUID, VaultDataModel>(mapName)
            .getAsync(uuid)
    }
}