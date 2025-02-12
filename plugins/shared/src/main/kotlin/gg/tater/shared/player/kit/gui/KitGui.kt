package gg.tater.shared.player.kit.gui

import gg.tater.shared.ARROW_TEXT
import gg.tater.shared.player.kit.KitDataModel
import gg.tater.shared.player.kit.KitPlayerDataModel
import gg.tater.shared.player.kit.KitService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Gui
import me.lucko.helper.menu.Item
import me.lucko.helper.time.DurationFormatter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant

class KitGui(
    player: Player,
    private val kits: Set<KitDataModel>,
    private val data: KitPlayerDataModel,
    private val redis: Redis,
    private val service: KitService = Services.load(KitService::class.java)
) :
    Gui(player, 3, "Select a Kit") {

    companion object {
        private val PLACEHOLDER_STACK: Item = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build(null)
    }

    override fun redraw() {
        for (slot in 0 until this.handle.size) {
            setItem(
                slot, PLACEHOLDER_STACK
            )
        }

        for (kit in kits) {
            setItem(
                kit.slot, ItemStackBuilder.of(kit.icon.clone())
                    .name("${kit.friendly} &7(Click)")
                    .lore(
                        " ",
                        "$ARROW_TEXT &bCooldown&f: ${DurationFormatter.format(Duration.ofMillis(kit.cooldown), true)}",
                        "$ARROW_TEXT &bLeft-Click &fto apply kit",
                        "$ARROW_TEXT &bRight-Click &fto view kit",
                        " "
                    )
                    .build({
                        close()
                        KitPreviewGui(player, kit, this).open()
                    }, {
                        close()

                        if (kit.permission.isNotEmpty() && !player.hasPermission(kit.permission)) {
                            player.sendMessage(Component.text("You cannot use this kit!", NamedTextColor.RED))
                            return@build
                        }

                        if (!data.canUse(kit)) {
                            val timeLeft = Duration.between(
                                Instant.now(),
                                Instant.ofEpochMilli(data.getLastUse(kit)!!.plus(kit.cooldown))
                            )

                            player.sendMessage(
                                Component.text(
                                    "You cannot use this kit for another ${
                                        DurationFormatter.format(
                                            timeLeft,
                                            true
                                        )
                                    }!", NamedTextColor.RED
                                )
                            )
                            return@build
                        }

                        val data = data.setLastUsed(kit)
                        service.save(player.uniqueId, data)

                        val inventory = player.inventory

                        for (stack in kit.contents) {
                            inventory.addItem(stack)
                        }

                        player.sendMessage(Component.text("Applied kit: ${kit.name}!", NamedTextColor.GREEN))
                    })
            )
        }
    }
}