package gg.tater.core.controllers.player.economy

import gg.tater.shared.DECIMAL_FORMAT
import gg.tater.shared.redis.Redis
import gg.tater.shared.player.economy.EconomyType
import gg.tater.shared.player.economy.PlayerEconomyModel
import me.lucko.helper.Commands
import me.lucko.helper.terminable.TerminableConsumer
import me.lucko.helper.terminable.module.TerminableModule
import net.luckperms.api.LuckPermsProvider

class EconomyController(private val redis: Redis) : TerminableModule {

    override fun setup(consumer: TerminableConsumer) {
        val perms = LuckPermsProvider.get()

        Commands.create()
            .assertPlayer()
            .handler { cmd ->
                val sender = cmd.sender()
                redis.economy().computeIfAbsentAsync(sender.uniqueId) {
                    PlayerEconomyModel(sender.uniqueId)
                }.thenAccept { cmd.reply("&aBalance&f: $${DECIMAL_FORMAT.format(it.get(EconomyType.MONEY))}") }
            }
            .registerAndBind(consumer, "bal", "balance", "money")

        Commands.create()
            .assertPermission("server.economy")
            .handler {
                val sub = it.arg(0).parseOrFail(String::class.java)
                val target = it.arg(1).parseOrFail(String::class.java)
                val amount = it.arg(2).parseOrFail(String::class.java).toDouble()

                perms.userManager.lookupUniqueId(target).thenAcceptAsync { uuid ->
                    if (uuid == null) {
                        it.reply("&cCould not find user with that name.")
                        return@thenAcceptAsync
                    }

                    val eco = redis.economy()[uuid]
                    if (eco == null) {
                        it.reply("&cError fetching eco data.")
                        return@thenAcceptAsync
                    }

                    if (sub.equals("give", true)) {
                        eco.add(EconomyType.MONEY, amount)
                        redis.economy()[uuid] = eco
                        it.reply("&aGave $${DECIMAL_FORMAT.format(amount)} to $target.")
                        return@thenAcceptAsync
                    }

                    if (sub.equals("take", true)) {
                        eco.sub(EconomyType.MONEY, amount)
                        redis.economy()[uuid] = eco
                        it.reply("&aTook $${DECIMAL_FORMAT.format(amount)} from $target.")
                        return@thenAcceptAsync
                    }

                    if (sub.equals("set", true)) {
                        eco.set(EconomyType.MONEY, amount)
                        redis.economy()[uuid] = eco
                        it.reply("&aSet $target's balance to $${DECIMAL_FORMAT.format(amount)}")
                    }
                }
            }
            .registerAndBind(consumer, "eco", "economy")
    }
}