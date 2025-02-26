package gg.tater.shared.player.kit.gui

import gg.tater.shared.player.kit.model.KitDataModel
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Gui
import me.lucko.helper.menu.Item
import me.lucko.helper.menu.scheme.MenuScheme
import me.lucko.helper.menu.scheme.StandardSchemeMappings
import org.bukkit.Material
import org.bukkit.entity.Player

class KitPreviewGui(player: Player, private val kit: KitDataModel, private val gui: KitGui) :
    Gui(player, 4, "&nKit: ${kit.name}") {

    companion object {
        private val SCHEME: MenuScheme = MenuScheme(StandardSchemeMappings.EMPTY)
            .masks("000000000")
            .masks("011111110")
            .masks("011111110")
            .masks("000000000")

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

        setFallbackGui { gui }

        val populator = SCHEME.newPopulator(this)

        for (stack in kit.contents) {
            populator.accept(ItemStackBuilder.of(stack).build(null))
        }
    }
}