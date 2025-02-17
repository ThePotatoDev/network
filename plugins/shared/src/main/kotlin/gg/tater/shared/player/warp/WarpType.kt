package gg.tater.shared.player.warp

import gg.tater.shared.network.server.ServerType
import me.lucko.helper.item.ItemStackBuilder
import org.bukkit.Material

enum class WarpType(
    val slot: Int,
    val icon: ItemStackBuilder,
    val serverType: ServerType,
    val description: List<String>
) {

    PVP(
        10,
        ItemStackBuilder.of(Material.DIAMOND_SWORD)
            .name("&cPvP &7(Click)"),
        ServerType.PVP,
        listOf("Fight to the death versus players!", "May only the best survive.")
    ),
    SPAWN(
        13,
        ItemStackBuilder.of(Material.REDSTONE_LAMP)
            .name("&3Spawn &7(Click)"),
        ServerType.SPAWN,
        listOf("Interact with players", "and use various npcs.")
    ),
    PLANETS(
        15,
        ItemStackBuilder.of(Material.END_STONE)
            .name("&bPlanets &7(Click)"),
        ServerType.PLANET,
        listOf("Progress your way through planets", "through mining to unlock", "special items and achievements.")
    )
}