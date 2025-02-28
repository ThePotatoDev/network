package gg.tater.core.player.economy

import gg.tater.core.DECIMAL_FORMAT
import gg.tater.core.annotation.Controller
import gg.tater.core.server.model.GameModeType
import gg.tater.core.player.economy.model.EconomyType
import gg.tater.core.player.economy.model.PlayerEconomyModel
import gg.tater.core.player.economy.model.PlayerEconomyService
import gg.tater.core.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import net.luckperms.api.LuckPermsProvider
import org.redisson.api.RFuture
import java.util.*

@Controller(
    id = "economy-controller"
)
class EconomyController(mode: GameModeType) : PlayerEconomyService {

    private val mapName = "${mode.id}_economy"

    private val redis = Services.load(Redis::class.java)

    override fun compute(uuid: UUID): RFuture<PlayerEconomyModel> {
        return redis.client.getMap<UUID, PlayerEconomyModel>(mapName)
            .computeIfAbsentAsync(uuid) {
                PlayerEconomyModel(uuid)
            }
    }

    override fun get(uuid: UUID): RFuture<PlayerEconomyModel> {
        return redis.client.getMap<UUID, PlayerEconomyModel>(mapName)
            .getAsync(uuid)
    }

    override fun getSync(uuid: UUID): PlayerEconomyModel? {
        return redis.client.getMap<UUID, PlayerEconomyModel>(mapName)[uuid]
    }

    override fun save(uuid: UUID, eco: PlayerEconomyModel): RFuture<Boolean> {
        return redis.client.getMap<UUID, PlayerEconomyModel>(mapName)
            .fastPutAsync(uuid, eco)
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(PlayerEconomyService::class.java, this)
        val perms = LuckPermsProvider.get()

        Commands.create()
            .assertPlayer()
            .handler { cmd ->
                val sender = cmd.sender()
                compute(sender.uniqueId).thenAccept {
                    cmd.reply(
                        "&aBalance&f: $${
                            DECIMAL_FORMAT.format(
                                it.get(
                                    EconomyType.MONEY
                                )
                            )
                        }"
                    )
                }
            }
            .registerAndBind(consumer, "bal", "balance", "money")

        Commands.create()
            .assertPlayer()
            .handler {
                val sender = it.sender()

                if (it.args().size != 2) {
                    it.reply("&cUsage: /pay <player> <amount>")
                    return@handler
                }

                val name = it.arg(0).parseOrFail(String::class.java)
                val amount = it.arg(1).parseOrFail(String::class.java).toDouble()

                get(sender.uniqueId).thenAcceptAsync { eco ->
                    val balance = eco.get(EconomyType.MONEY)

                    if (balance - amount < 0) {
                        it.reply("&cYou do not have enough money to pay that amount!")
                        return@thenAcceptAsync
                    }

                    val target = perms.userManager.lookupUniqueId(name).join()

                    if (target == null) {
                        it.reply("&cCould not find existing player with that name.")
                        return@thenAcceptAsync
                    }

                    val targetEco = getSync(target) ?: return@thenAcceptAsync

                    eco.withdraw(EconomyType.MONEY, amount)
                    targetEco.add(EconomyType.MONEY, amount)

                    save(target, targetEco)
                    save(sender.uniqueId, eco)

                    it.reply("&aYou paid $target $${DECIMAL_FORMAT.format(amount)}!")
                }
            }
            .registerAndBind(consumer, "pay")

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

                    val eco = getSync(uuid)
                    if (eco == null) {
                        it.reply("&cError fetching eco data.")
                        return@thenAcceptAsync
                    }

                    if (sub.equals("give", true)) {
                        eco.add(EconomyType.MONEY, amount)
                        save(uuid, eco)
                        it.reply("&aGave $${DECIMAL_FORMAT.format(amount)} to $target.")
                        return@thenAcceptAsync
                    }

                    if (sub.equals("take", true)) {
                        eco.sub(EconomyType.MONEY, amount)
                        save(uuid, eco)
                        it.reply("&aTook $${DECIMAL_FORMAT.format(amount)} from $target.")
                        return@thenAcceptAsync
                    }

                    if (sub.equals("set", true)) {
                        eco.set(EconomyType.MONEY, amount)
                        save(uuid, eco)
                        it.reply("&aSet $target's balance to $${DECIMAL_FORMAT.format(amount)}")
                    }
                }
            }
            .registerAndBind(consumer, "eco", "economy")
    }
}