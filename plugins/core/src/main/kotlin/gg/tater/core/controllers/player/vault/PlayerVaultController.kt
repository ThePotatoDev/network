package gg.tater.core.controllers.player.vault

import gg.tater.shared.ARROW_TEXT
import gg.tater.shared.redis.Redis
import gg.tater.shared.player.vault.VaultDataModel
import gg.tater.shared.player.vault.gui.VaultGuiItem
import gg.tater.shared.player.vault.gui.VaultItemGui
import gg.tater.shared.player.vault.gui.VaultSelectionGui
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryCloseEvent

class PlayerVaultController(private val redis: Redis) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

                redis.vaults().computeIfAbsentAsync(sender.uniqueId) { VaultDataModel(sender.uniqueId) }
                    .thenAccept { data ->
                        Schedulers.sync().run {
                            if (it.args().isEmpty()) {
                                val amount = data.amount
                                val items: MutableList<VaultGuiItem> = mutableListOf()

                                for (i in 0 until amount) {
                                    val id = i + 1

                                    items.add(
                                        VaultGuiItem(id, ItemStackBuilder.of(Material.ENDER_CHEST)
                                            .name("&3&lVault #$id")
                                            .lore(
                                                " ",
                                                "$ARROW_TEXT &7Click to open this vault",
                                                " "
                                            )
                                            .build { VaultItemGui(sender, i, data.getVaultItems(i)).open() })
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

                            if (index + 1 > data.amount) {
                                it.reply("&cYou do not have access to that vault!")
                                return@run
                            }

                            VaultItemGui(sender, index, data.getVaultItems(index)).open()
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

                redis.vaults().getAsync(player.uniqueId).thenAccept { data ->
                    data.setVaultItems(id, inventory.contents)
                    redis.vaults().fastPutAsync(player.uniqueId, data)
                }

                player.performCommand("pv")
            }
            .bindWith(consumer)
    }
}