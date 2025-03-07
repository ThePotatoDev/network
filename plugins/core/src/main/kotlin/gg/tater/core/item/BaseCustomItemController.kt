package gg.tater.core.item

import gg.tater.core.annotation.Controller
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.item.ItemStackBuilder
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.Material
import org.bukkit.entity.Player

@Controller(
    id = "base-item-controller"
)
class BaseCustomItemController : CustomItemService {

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(CustomItemService::class.java, this)

        Commands.create()
            .assertPermission("server.givecustomitem")
            .handler {
                if (it.args().size < 4) {
                    it.reply("&cUsage: /givecustomitem <target> <material> <id> <display>")
                    return@handler
                }

                val target = it.arg(0).parseOrFail(Player::class.java)
                val material = Material.valueOf(it.arg(1).parseOrFail(String::class.java))
                val id = it.arg(2).parseOrFail(String::class.java).toInt()
                val display = it.arg(3).parseOrFail(String::class.java)

                val stack = ItemStackBuilder.of(material)
                    .name(display)
                    .transformMeta { meta -> meta.setCustomModelData(id) }
                    .build()

                target.inventory.addItem(stack)
                it.reply("&aGave custom item to ${target.name}")
            }
            .registerAndBind(consumer, "givecustomitem")
    }
}