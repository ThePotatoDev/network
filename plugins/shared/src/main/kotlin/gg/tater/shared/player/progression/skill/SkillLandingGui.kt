package gg.tater.shared.player.progression.skill

import gg.tater.shared.player.progression.PlayerProgressDataModel
import gg.tater.shared.player.progression.skill.model.SkillType
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Gui
import me.lucko.helper.menu.scheme.MenuScheme
import org.bukkit.Material
import org.bukkit.entity.Player

class SkillLandingGui(player: Player, private val data: PlayerProgressDataModel) : Gui(player, 5, "Your Skills") {

    companion object {
        private val ITEM_SCHEME: MenuScheme = MenuScheme()
            .mask("000000000")
            .mask("000010000")
            .mask("000101000")
            .mask("001010100")
            .mask("000000000")

        private val NUMERALS = listOf(
            1000 to "M",
            900 to "CM",
            500 to "D",
            400 to "CD",
            100 to "C",
            90 to "XC",
            50 to "L",
            40 to "XL",
            10 to "X",
            9 to "IX",
            5 to "V",
            4 to "IV",
            1 to "I"
        )
    }

    override fun redraw() {
        for (slot in 0 until this.handle.size) {
            setItem(
                slot, ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build(null)
            )
        }

        val populator = ITEM_SCHEME.newPopulator(this)
        for (skill in SkillType.entries) {
            val level = data.getLevel(skill)

            populator.accept(skill.icon
                .name(skill.friendly + " &7(Level ${toRomanNumeral(level)})")
                .lore(
                    " ",
                    "",
                    " "
                )
                .build {
                    SkillSpecificGui(player, skill, data).open()
                })
        }
    }

    private fun toRomanNumeral(number: Int): String {
        if (number <= 0) return ""

        var num = number
        val result = StringBuilder()

        for ((value, numeral) in NUMERALS) {
            while (num >= value) {
                result.append(numeral)
                num -= value
            }
        }

        return result.toString()
    }
}