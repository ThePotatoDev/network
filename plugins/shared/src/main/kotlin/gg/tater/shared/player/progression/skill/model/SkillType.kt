package gg.tater.shared.player.progression.skill.model

import me.lucko.helper.item.ItemStackBuilder
import org.bukkit.Material

enum class SkillType(val friendly: String, val icon: ItemStackBuilder) {

    MINING("&aMining Skill", ItemStackBuilder.of(Material.DIAMOND_PICKAXE)),
    FARMING("&bFarming Skill", ItemStackBuilder.of(Material.WOODEN_HOE)),
    FISHING("&eFishing Skill", ItemStackBuilder.of(Material.FISHING_ROD)),
    WOODCUTTING("&dWoodcutting Skill", ItemStackBuilder.of(Material.OAK_LOG)),
    PVE("&2PvE Skill", ItemStackBuilder.of(Material.ROTTEN_FLESH)),
    CRAFTING("&3Crafting Skill", ItemStackBuilder.of(Material.CRAFTING_TABLE))
}