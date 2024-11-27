package gg.tater.shared.player.progression.skill

import gg.tater.shared.player.progression.PlayerProgressDataModel
import gg.tater.shared.player.progression.skill.model.SkillType
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Gui
import org.bukkit.Material
import org.bukkit.entity.Player

class SkillSpecificGui(player: Player, type: SkillType, private val data: PlayerProgressDataModel) :
    Gui(player, 6, type.friendly) {

    override fun redraw() {
        setFallbackGui { SkillLandingGui(player, data) }

        for (slot in 0 until this.handle.size) {
            setItem(
                slot, ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build(null)
            )
        }
    }
}