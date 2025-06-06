package gg.tater.oneblock.island.phase.model

import me.lucko.helper.item.ItemStackBuilder
import org.bukkit.Material

data class OneBlockPhase(
    val id: Int,
    val friendly: String,
    val threshold: Int,
    val icon: ItemStackBuilder,
    val blocks: List<Pair<Material, Int>>,
    val progressiveRewardCommands: Map<Int, List<String>> = mapOf(),
    val completionRewardCommands: List<String> = listOf()
)
