package gg.tater.shared.island.setting.model

import org.bukkit.Material

enum class IslandSettingType(val friendly: String, val icon: Material, val default: Boolean) {

    SPAWN_ANIMALS("Spawn Animals", Material.CHICKEN_SPAWN_EGG, true),
    SPAWN_MOBS("Spawn Mobs", Material.ZOMBIE_HEAD, true),
    PVP("PvP", Material.IRON_SWORD, false),
    FIRE_SPREAD("Fire Spread", Material.CAMPFIRE, false),
    EXPLOSIONS("Explosions", Material.TNT, false),
    RAINING("Raining", Material.CAULDRON, true),
    LOCKED("Lock Island", Material.TRIPWIRE_HOOK, false)

}