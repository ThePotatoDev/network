package gg.tater.shared.player.chat.tag

import me.lucko.helper.menu.Item
import me.lucko.helper.menu.paginated.PaginatedGui
import me.lucko.helper.menu.paginated.PaginatedGuiBuilder
import org.bukkit.entity.Player
import java.util.function.Function

class ChatTagGui(content: Function<PaginatedGui, MutableList<Item>>, player: Player, model: PaginatedGuiBuilder) : PaginatedGui(content, player,
    model
) {
}