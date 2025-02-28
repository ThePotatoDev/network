package gg.tater.core.island.flag

import gg.tater.core.ARROW_TEXT
import gg.tater.core.island.Island
import gg.tater.core.island.IslandService
import gg.tater.core.island.flag.model.FlagType
import gg.tater.core.island.message.IslandUpdateRequest
import gg.tater.core.island.player.IslandPlayer
import gg.tater.core.redis.Redis
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Gui
import me.lucko.helper.menu.scheme.MenuScheme
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player

class IslandFlagGui<T : Island, K : IslandPlayer>(
    player: Player,
    private val island: T,
    private val redis: Redis = Services.load(Redis::class.java),
    private val islands: IslandService<T, K> = Services.load(IslandService::class.java) as IslandService<T, K>
) :
    Gui(player, 5, "Island Flags") {

    companion object {
        private val ITEM_SCHEME = MenuScheme()
            .mask("000000000")
            .mask("111111111")
            .mask("111111111")
            .mask("111111111")
            .mask("111111111")
            .mask("000000000")

        private fun getFlagLore(flag: FlagType, current: Island.Role): List<String> {
            val lore: MutableList<String> = mutableListOf()
            lore.add(" ")
            lore.addAll(flag.description)
            lore.add(" ")

            for (each in Island.Role.entries) {
                if (current == each) {
                    lore.add("$ARROW_TEXT &a${each.friendly}")
                } else {
                    lore.add("$ARROW_TEXT &7${each.friendly}")
                }
            }

            lore.add(" ")
            lore.add("&bClick &fto cycle.")

            return lore
        }
    }

    override fun redraw() {
        bind(AutoCloseable {
            player.performCommand("is")
        })

        val populator = ITEM_SCHEME.newPopulator(this)

        for (flag in FlagType.entries) {
            val currentRole = island.getFlagRole(flag)

            populator.accept(
                ItemStackBuilder.of(flag.icon)
                    .name("&b${flag.friendly}")
                    .lore(getFlagLore(flag, currentRole))
                    .build {
                        if (!island.canInteract(player.uniqueId, FlagType.CHANGE_FLAGS)) {
                            player.sendMessage(
                                Component.text(
                                    "You are not allowed to change flags on this island!",
                                    NamedTextColor.RED
                                )
                            )
                            return@build
                        }

                        island.setFlagRole(flag, currentRole.next())
                        redraw()

                        Schedulers.async().run {
                            islands.transaction({ map -> map[island.id] = island }, onSuccess = {
                                redis.publish(IslandUpdateRequest(island))
                            })
                        }
                    })
        }

        for (index in 0 until this.handle.size) {
            val slot = getSlot(index)
            if (slot.item != null) continue
            slot.setItem(
                ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                    .name(" ")
                    .build()
            )
        }
    }
}