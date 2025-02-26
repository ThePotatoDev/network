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
        11,
        ItemStackBuilder.of(Material.DIAMOND_SWORD)
            .name("&cPvP Warp &7(Click)"),
        ServerType.ONEBLOCK_PVP,
        listOf("&7&oFight to the death versus players!", "&7&oMay only the best survive.")
    ),
    SPAWN(
        13,
        ItemStackBuilder.of(Material.REDSTONE_LAMP)
            .name("&3Spawn Warp &7(Click)"),
        ServerType.ONEBLOCK_SPAWN,
        listOf("&7&oInteract with players", "&7&oand use various npcs.")
    ),
    PLANETS(
        15,
        ItemStackBuilder.of(Material.END_STONE)
            .name("&bPlanets Warp &7(Click)"),
        ServerType.PLANET,
        listOf(
            "&7&oProgress your way through planets",
            "&7&othrough mining to unlock",
            "&7&ospecial items and achievements."
        )
    );
}