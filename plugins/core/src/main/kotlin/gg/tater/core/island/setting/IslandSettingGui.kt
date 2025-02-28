package gg.tater.core.island.setting

import gg.tater.core.ARROW_TEXT
import gg.tater.core.island.Island
import gg.tater.core.island.IslandService
import gg.tater.core.island.flag.model.FlagType
import gg.tater.core.island.message.IslandUpdateRequest
import gg.tater.core.island.player.IslandPlayer
import gg.tater.core.island.setting.model.IslandSettingType
import gg.tater.core.redis.Redis
import me.lucko.helper.Schedulers
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Gui
import me.lucko.helper.menu.scheme.MenuScheme
import me.lucko.helper.menu.scheme.StandardSchemeMappings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class IslandSettingGui<T : Island, K : IslandPlayer>(
    player: Player,
    private val island: T,
    private val redis: Redis = Services.load(Redis::class.java),
    private val islands: IslandService<T, K> = Services.load(IslandService::class.java) as IslandService<T, K>
) : Gui(player, 4, "Island Settings") {

    companion object {
        private val PANE_SCHEME = MenuScheme(StandardSchemeMappings.STAINED_GLASS)
            .mask("111111111")
            .mask("100000001")
            .mask("100000001")
            .mask("111111111")
            .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0)
            .scheme(0, 0)
            .scheme(0, 0)
            .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0)

        private val ITEM_SCHEME = MenuScheme()
            .mask("000000000")
            .mask("001010100")
            .mask("010101010")
            .mask("000000000")
    }

    override fun redraw() {
        bind(AutoCloseable {
            player.performCommand("is")
        })

        PANE_SCHEME.apply(this)
        val populator = ITEM_SCHEME.newPopulator(this)

        for (setting in IslandSettingType.entries) {
            populator.accept(
                ItemStackBuilder.of(setting.icon)
                    .name("&f${setting.friendly}: ${if (island.getSettingValue(setting)) "&aEnabled" else "&cDisabled"}")
                    .lore(
                        " ",
                        "$ARROW_TEXT &bClick &7to toggle!",
                        " "
                    )
                    .build {
                        if (!island.canInteract(player.uniqueId, FlagType.CHANGE_SETTINGS)) {
                            player.sendMessage(
                                Component.text(
                                    "You cannot change settings on this island!",
                                    NamedTextColor.RED
                                )
                            )
                            return@build
                        }

                        val newValue = !island.getSettingValue(setting)
                        island.updateSetting(setting, newValue)
                        redraw()

                        if (newValue) {
                            player.sendMessage(
                                Component.text(
                                    "You have enabled the ${setting.friendly} island setting!",
                                    NamedTextColor.GREEN
                                )
                            )
                        } else {
                            player.sendMessage(
                                Component.text(
                                    "You have disabled the ${setting.friendly} island setting!",
                                    NamedTextColor.RED
                                )
                            )
                        }

                        Schedulers.async().run {
                            islands.transaction(
                                { map -> map[island.id] = island },
                                onSuccess = {
                                    redis.publish(IslandUpdateRequest(island))
                                })
                        }
                    })
        }
    }
}