package gg.tater.core.island.flag.model

import gg.tater.core.island.Island
import org.bukkit.Material

enum class FlagType(
    val defaultRole: Island.Role,
    val icon: Material,
    val description: List<String>,
    val friendly: String
) {

    BREAK_BLOCKS(
        Island.Role.MEMBER,
        Material.IRON_PICKAXE,
        listOf("&7Ability to break blocks on this island."),
        "Break Blocks"
    ),

    PLACE_BLOCKS(
        Island.Role.MEMBER,
        Material.BRICKS,
        listOf("&7Ability to place blocks on this island."),
        "Place Blocks"
    ),

    BREAK_SPAWNERS(
        Island.Role.MEMBER,
        Material.SPAWNER,
        listOf("&7Ability to break spawners on this island."),
        "Break Spawners"
    ),

    INTERACT_BLOCKS(
        Island.Role.MEMBER,
        Material.BONE_BLOCK,
        listOf("&7Ability to interact with blocks on this island."),
        "Block Interaction"
    ),

    DAMAGE_MOBS(
        Island.Role.MEMBER,
        Material.DIAMOND_SWORD,
        listOf("&7Ability to damage mobs on this island."),
        "Damage Mobs"
    ),

    DAMAGE_ANIMALS(
        Island.Role.MEMBER,
        Material.IRON_SWORD,
        listOf("&7Ability to damage animals on this island."),
        "Damage Animals"
    ),

    USE_DOORS(
        Island.Role.MEMBER, Material.OAK_DOOR,
        listOf("&7Ability to use doors on this island."),
        "Use Doors"
    ),

    USE_BUTTOMS(
        Island.Role.MEMBER,
        Material.OAK_BUTTON,
        listOf("&7Ability to use buttons on this island."),
        "Use Buttons"
    ),

    USE_LEVERS(
        Island.Role.MEMBER,
        Material.LEVER,
        listOf("&7Ability to use levers on this island."),
        "Use Levers"
    ),

    USE_PRESSURE_PLATES(
        Island.Role.MEMBER,
        Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
        listOf("&7Ability to use pressure plates on this island."),
        "Use Pressure Plates"
    ),

    OPEN_CONTAINERS(
        Island.Role.MEMBER,
        Material.CHEST,
        listOf("&7Ability to use containers on this island."),
        "Open Containers"
    ),

    INVITE_PLAYERS(
        Island.Role.CO_OWNER,
        Material.PAPER,
        listOf("&7Ability to invite players to this island."),
        "Invite Players"
    ),

    KICK_PLAYERS(
        Island.Role.CO_OWNER,
        Material.CLOCK,
        listOf("&7Ability to kick players from this island."),
        "Kick Players"
    ),

    PROMOTE_DEMOTE_PLAYERS(
        Island.Role.CO_OWNER,
        Material.PLAYER_HEAD,
        listOf("&7Ability to promote/demote players on this island."),
        "Promote/Demote Players"
    ),

    BAN_PLAYERS(
        Island.Role.CO_OWNER,
        Material.SKELETON_SKULL,
        listOf("&7Ability to ban players from this island."),
        "Ban Players"
    ),

    SET_WARPS(
        Island.Role.CO_OWNER,
        Material.ENDER_EYE,
        listOf("&7Ability to set warps on this island."),
        "Set Warps"
    ),

    SET_HOME_WARP(
        Island.Role.OWNER,
        Material.RED_BED,
        listOf("&7Ability to set the home warp for this island."),
        "Set Home Warp"
    ),

    DROP_ITEMS(
        Island.Role.MEMBER,
        Material.EGG,
        listOf("&7Ability to drop items on this island."),
        "Drop Items"
    ),

    PICKUP_ITEMS(
        Island.Role.MEMBER,
        Material.STRING,
        listOf("&7Ability to pickup items on this island."),
        "Pickup Items"
    ),

    USE_ENDERPEARLS(
        Island.Role.MEMBER,
        Material.ENDER_PEARL,
        listOf("&7Ability to use ender pearls on this island."),
        "Use Enderpearls"
    ),

    VILLAGER_TRADING(
        Island.Role.MEMBER,
        Material.VILLAGER_SPAWN_EGG,
        listOf("&7Ability to trade with villagers on this island"),
        "Villager Trading"
    ),

    CHANGE_FLAGS(
        Island.Role.CO_OWNER,
        Material.RED_BANNER,
        listOf("&7Ability to change flags on this island"),
        "Change Flags"
    ),

    CHANGE_SETTINGS(
        Island.Role.CO_OWNER,
        Material.MAP,
        listOf("&7Ability to change settings on this island"),
        "Change Flags"
    ),

    PLAYER_SHOPS(
        Island.Role.CO_OWNER,
        Material.ENDER_CHEST,
        listOf("&7Ability to create/delete player shops on this island"),
        "Player Shops"
    ),
    ;

}