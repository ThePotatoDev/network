//package gg.tater.shared.player.chat.color
//
//import gg.tater.core.ARROW_TEXT
//import gg.tater.core.MINI_MESSAGE
//import gg.tater.core.player.PlayerDataModel
//import me.lucko.helper.Services
//import me.lucko.helper.item.ItemStackBuilder
//import me.lucko.helper.menu.paginated.PageInfo
//import me.lucko.helper.menu.paginated.PaginatedGui
//import me.lucko.helper.menu.paginated.PaginatedGuiBuilder
//import me.lucko.helper.menu.scheme.MenuScheme
//import me.lucko.helper.menu.scheme.StandardSchemeMappings
//import net.kyori.adventure.text.Component
//import net.kyori.adventure.text.format.NamedTextColor
//import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
//import org.bukkit.Material
//import org.bukkit.entity.Player
//
//class ChatColorGui(
//    player: Player,
//    private val data: PlayerDataModel,
//    private val players: PlayerService = Services.load(PlayerService::class.java)
//) :
//    PaginatedGui(
//        { gui ->
//            gui.setItem(
//                49, ItemStackBuilder.of(Material.REDSTONE)
//                    .name("&cReset Chat Color")
//                    .lore(
//                        " ",
//                        "$ARROW_TEXT &bClick &fto reset!",
//                        " ",
//                    )
//                    .build {
//                        gui.close()
//                        if (data.chatColor == null) {
//                            player.sendMessage(Component.text("You do not have a chat color.", NamedTextColor.RED))
//                            return@build
//                        }
//
//                        data.chatColor = null
//                        players.save(data)
//                        player.sendMessage(Component.text("You have reset your chat color!", NamedTextColor.RED))
//                    })
//
//            ChatColor.CHAT_COLORS.map {
//                val color = it.value
//
//                ItemStackBuilder.of(Material.PAPER)
//                    .name(
//                        LegacyComponentSerializer.legacyAmpersand()
//                            .serialize(MINI_MESSAGE.deserialize("<gradient:${color.startColor}:${color.endColor}>${color.name} Chat Color</gradient>"))
//                    )
//                    .lore(
//                        " ",
//                        "$ARROW_TEXT &bClick &fto apply!",
//                        " "
//                    )
//                    .build {
//                        if (!player.hasPermission("chatcolor.${it.key}")) {
//                            player.sendMessage(
//                                Component.text(
//                                    "You do not have permission to use this chat color!",
//                                    NamedTextColor.RED
//                                )
//                            )
//                            return@build
//                        }
//
//                        gui.close()
//                        data.chatColor = color
//                        players.save(data)
//                        player.sendMessage(Component.text("Selected chat color: ${it.key}!", NamedTextColor.GREEN))
//                    }
//            }
//        }, player, BUILDER
//    ) {
//
//    companion object {
//        private val BUILDER: PaginatedGuiBuilder = PaginatedGuiBuilder
//            .create()
//            .title("&nChat Colors")
//            .nextPageSlot(53)
//            .previousPageSlot(45)
//            .lines(6)
//            .scheme(
//                MenuScheme(StandardSchemeMappings.STAINED_GLASS)
//                    .maskEmpty(5)
//                    .mask("111101111")
//                    .scheme(0, 0, 0, 0, 0, 0, 0, 0)
//            )
//            .nextPageItem { info: PageInfo ->
//                ItemStackBuilder.of(Material.PAPER)
//                    .name("&6Next Page &f(" + info.current + "/" + info.size + ")")
//                    .lore("&7&oClick to advance.")
//                    .build()
//            }
//            .previousPageItem { info: PageInfo ->
//                ItemStackBuilder.of(Material.PAPER)
//                    .name("&cPrevious Page &f(" + info.current + "/" + info.size + ")")
//                    .lore("&7&oClick to return.")
//                    .build()
//            }
//            .itemSlots(
//                MenuScheme()
//                    .mask("111111111")
//                    .mask("111111111")
//                    .mask("111111111")
//                    .mask("111111111")
//                    .mask("111111111")
//                    .mask("000000000")
//                    .maskedIndexesImmutable
//            )
//    }
//}