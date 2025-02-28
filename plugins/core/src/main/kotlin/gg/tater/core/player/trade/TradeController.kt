package gg.tater.core.player.trade

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import gg.tater.core.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.luckperms.api.LuckPermsProvider
import org.bukkit.inventory.ItemStack
import java.util.*

class TradeController(private val redis: Redis) : TerminableModule, TradeService {

    private val offerings: Multimap<UUID, ItemStack> = ArrayListMultimap.create()

    override fun setup(consumer: TerminableConsumer) {
        val perms = LuckPermsProvider.get()

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()
                if (it.args().isEmpty()) {
                    it.reply("&cChoose a player to send a trade request to.")
                    return@handler
                }

                val target = it.arg(0).parseOrFail(String::class.java)
                perms.userManager.lookupUniqueId(target)
                    .thenAcceptAsync { targetId ->
                        if (targetId == null) {
                            it.reply("&cCould not find user.")
                            return@thenAcceptAsync
                        }

                        val entry = TradeEntry(sender.uniqueId, targetId)
                    }
            }
            .registerAndBind(consumer, "trade")
    }
}