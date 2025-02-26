package gg.tater.shared.island.gui

import com.destroystokyo.paper.profile.ProfileProperty
import gg.tater.shared.ARROW_TEXT
import gg.tater.shared.island.Island
import gg.tater.shared.island.IslandService
import gg.tater.shared.island.flag.model.FlagType
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.menu.Item
import me.lucko.helper.menu.paginated.PageInfo
import me.lucko.helper.menu.paginated.PaginatedGui
import me.lucko.helper.menu.paginated.PaginatedGuiBuilder
import me.lucko.helper.menu.scheme.MenuScheme
import me.lucko.helper.menu.scheme.StandardSchemeMappings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

class IslandMemberGui<T: Island>(
    player: Player,
    private val island: T,
    private val offline: Map<UUID, Pair<String, String>>
) :
    PaginatedGui(
        {
            getItems(it, island, offline)
        },
        player, BUILDER
    ) {

    companion object {
        private val BUILDER: PaginatedGuiBuilder = PaginatedGuiBuilder
            .create()
            .title("&nIsland Members")
            .nextPageSlot(53)
            .previousPageSlot(45)
            .lines(6)
            .scheme(
                MenuScheme(StandardSchemeMappings.STAINED_GLASS)
                    .maskEmpty(5)
                    .mask("111111111")
                    .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0)
            )
            .nextPageItem { info: PageInfo ->
                ItemStackBuilder.of(Material.PAPER)
                    .name("&6Next Page &f(" + info.current + "/" + info.size + ")")
                    .lore("&7&oClick to advance.")
                    .build()
            }
            .previousPageItem { info: PageInfo ->
                ItemStackBuilder.of(Material.PAPER)
                    .name("&cPrevious Page &f(" + info.current + "/" + info.size + ")")
                    .lore("&7&oClick to return.")
                    .build()
            }
            .itemSlots(
                MenuScheme()
                    .mask("111111111")
                    .mask("111111111")
                    .mask("111111111")
                    .mask("111111111")
                    .mask("111111111")
                    .mask("000000000")
                    .maskedIndexesImmutable
            )

        private fun getLore(current: Island.Role): List<String> {
            val lore: MutableList<String> = mutableListOf()
            lore.add(" ")
            lore.add("&eCurrent Role&f: ${current.friendly}")
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

        private fun <T: Island> getItems(
            gui: PaginatedGui,
            island: T,
            offline: Map<UUID, Pair<String, String>>,
            islands: IslandService<T> = Services.load(IslandService::class.java) as IslandService<T>
        ): List<Item> {
            val player = gui.player

            gui.bind(AutoCloseable {
                player.performCommand("is")
            })

            return island.members.entries
                .sortedByDescending { entry -> entry.value.hierarchy }
                .map { member ->
                    val data = offline[member.key]!!
                    val currentRole = member.value

                    ItemStackBuilder.of(Material.PLAYER_HEAD)
                        .name("&6${data.first} &7(${currentRole.friendly}&7)")
                        .lore(getLore(currentRole))
                        .transformMeta { meta ->
                            val skullMeta = meta as SkullMeta
                            val profile = Bukkit.createProfile(UUID.randomUUID())
                            profile.setProperty(ProfileProperty("textures", data.second))
                            skullMeta.playerProfile = profile
                        }
                        .build {
                            if (!island.canInteract(player.uniqueId, FlagType.PROMOTE_DEMOTE_PLAYERS)) {
                                player.sendMessage(Component.text("You do not have permission to promote/demote players!"))
                                return@build
                            }

                            if (island.ownerId == member.key) {
                                player.sendMessage(
                                    Component.text(
                                        "You cannot change the island owners role!",
                                        NamedTextColor.RED
                                    )
                                )
                                return@build
                            }

                            island.setRoleFor(member.key, currentRole.next())

                            islands.transaction({ map -> map[island.id] = island }, onSuccess = {
                                gui.updateContent(getItems(gui, island, offline))
                                gui.redraw()
                            })
                        }
                }
        }
    }
}