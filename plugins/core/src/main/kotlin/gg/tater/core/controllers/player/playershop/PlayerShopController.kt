package gg.tater.core.controllers.player.playershop

import gg.tater.shared.island.IslandService
import gg.tater.shared.island.flag.model.FlagType
import gg.tater.shared.player.chat.ScopedChatPrompt
import gg.tater.shared.player.playershop.PlayerShopDataModel
import gg.tater.shared.player.playershop.PlayerShopGui
import gg.tater.shared.player.playershop.PlayerShopService
import gg.tater.shared.player.position.WrappedPosition
import gg.tater.shared.redis.Redis
import me.lucko.helper.Commands
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import net.jodah.expiringmap.ExpirationPolicy
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.redisson.api.RFuture
import java.util.*
import java.util.concurrent.TimeUnit

class PlayerShopController(private val redis: Redis, private val service: IslandService, private val server: String) :
    PlayerShopService {

    companion object {
        private const val PLAYER_SHOP_MAP_NAME = "player_shops"

        val NAME_PROMPT = object :
            ScopedChatPrompt(
                PromptConfig(
                    ExpirationPolicy.CREATED,
                    TimeUnit.SECONDS,
                    30L,
                    setOf("exit"),
                    listOf(
                        Component.text("Please enter the name that you want to set for your player shop.")
                            .color(NamedTextColor.GREEN)
                    )
                )
            ) {
            override fun onExpire(target: Player) {
                target.sendMessage(
                    Component.text("Your player shop alter time period has expired.")
                        .color(NamedTextColor.RED)
                )
            }
        }

        val DESC_PROMPT = object :
            ScopedChatPrompt(
                PromptConfig(
                    ExpirationPolicy.CREATED,
                    TimeUnit.SECONDS,
                    30L,
                    setOf("exit"),
                    listOf(
                        Component.text("Please enter the description that you want to set for your player shop.")
                            .color(NamedTextColor.GREEN)
                    )
                )
            ) {
            override fun onExpire(target: Player) {
                target.sendMessage(
                    Component.text("Your player shop alter time period has expired.")
                        .color(NamedTextColor.RED)
                )
            }
        }
    }

    override fun all(): RFuture<Collection<PlayerShopDataModel>> {
        return redis.client.getMap<UUID, PlayerShopDataModel>(PLAYER_SHOP_MAP_NAME)
            .readAllValuesAsync()
    }

    override fun get(uuid: UUID): RFuture<PlayerShopDataModel> {
        return redis.client.getMap<UUID, PlayerShopDataModel>(PLAYER_SHOP_MAP_NAME)
            .getAsync(uuid)
    }

    override fun save(uuid: UUID, shop: PlayerShopDataModel): RFuture<Boolean> {
        return redis.client.getMap<UUID, PlayerShopDataModel>(PLAYER_SHOP_MAP_NAME)
            .fastPutAsync(uuid, shop)
    }

    override fun delete(uuid: UUID): RFuture<Long> {
        return redis.client.getMap<UUID, PlayerShopDataModel>(PLAYER_SHOP_MAP_NAME)
            .fastRemoveAsync(uuid)
    }

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(PlayerShopService::class.java, this)

        consumer.bindModule(DESC_PROMPT)
        consumer.bindModule(NAME_PROMPT)

        Commands.create()
            .assertPlayer()
            .tabHandler {
                if (it.args().size == 1) {
                    return@tabHandler listOf("create", "delete", "setname", "setdesc", "seticon")
                }

                emptyList<String>()
            }
            .handler {
                val sender = it.sender()

                if (it.args().isEmpty()) {
                    all().thenAcceptAsync { shops -> PlayerShopGui(sender, shops, redis, server).open() }
                    return@handler
                }

                val world = sender.world
                val arg = it.arg(0).parseOrFail(String::class.java)

                get(sender.uniqueId).thenAccept { shop ->
                    if (arg.equals("create", true)) {
                        if (it.args().size != 1) {
                            it.reply("&cUsage: /pshop create")
                            return@thenAccept
                        }

                        if (shop != null) {
                            it.reply("&cYou already have a player shop created!")
                            return@thenAccept
                        }

                        val island = service.getIsland(world)
                        if (island == null) {
                            it.reply("&cYou cannot create a player shop here because there is no island present!")
                            return@thenAccept
                        }

                        if (!island.canInteract(sender.uniqueId, FlagType.PLAYER_SHOPS)) {
                            it.reply("&cYou are not allowed to alter player shops on this island!")
                            return@thenAccept
                        }

                        val hand = sender.inventory.itemInMainHand
                        if (hand.type == Material.AIR) {
                            it.reply("&cPlease hold a valid item for the player shop icon.")
                            return@thenAccept
                        }

                        NAME_PROMPT.start(sender) { name, namePrompt ->
                            namePrompt.end(sender)

                            DESC_PROMPT.start(sender) { desc, descPrompt ->
                                descPrompt.end(sender)
                                save(
                                    sender.uniqueId,
                                    PlayerShopDataModel(name, desc, hand, island.id, WrappedPosition(sender.location))
                                )
                                it.reply("&aPlayer shop created successfully.")
                            }
                        }
                        return@thenAccept
                    }

                    if (arg.equals("delete", true)) {
                        delete(sender.uniqueId)
                        it.reply("&cYour player shop has been deleted.")
                        return@thenAccept
                    }

                    if (arg.equals("setspawn", true)) {
                        if (shop == null) {
                            it.reply("&cYou do not have a player shop.")
                            return@thenAccept
                        }

                        val island = service.getIsland(sender.world)
                        if (island == null) {
                            it.reply("&cThere is not an island present at your location.")
                            return@thenAccept
                        }

                        if (!island.canInteract(sender.uniqueId, FlagType.PLAYER_SHOPS)) {
                            it.reply("&cYou do not have permission to alter player shops on this island.")
                            return@thenAccept
                        }

                        shop.position = WrappedPosition(sender.location)
                        save(sender.uniqueId, shop)
                        it.reply("&aPlayer shop spawn has been set.")
                        return@thenAccept
                    }

                    if (arg.equals("setname", true)) {
                        if (shop == null) {
                            it.reply("&cYou do not have a player shop.")
                            return@thenAccept
                        }

                        NAME_PROMPT.start(sender) { name, prompt ->
                            prompt.end(sender)
                            shop.name = name
                            save(sender.uniqueId, shop)
                        }

                        it.reply("&aPlayer shop name has been set.")
                        return@thenAccept
                    }

                    if (arg.equals("setdesc", true)) {
                        if (shop == null) {
                            it.reply("&cYou do not have a player shop.")
                            return@thenAccept
                        }

                        DESC_PROMPT.start(sender) { desc, prompt ->
                            prompt.end(sender)
                            shop.description = desc
                            save(sender.uniqueId, shop)
                        }

                        it.reply("&aPlayer shop description has been set.")
                        return@thenAccept
                    }

                    if (arg.equals("seticon", true)) {
                        if (shop == null) {
                            it.reply("&cYou do not have a player shop.")
                            return@thenAccept
                        }

                        val hand = sender.inventory.itemInMainHand
                        if (hand.type == Material.AIR) {
                            it.reply("&cPlease hold a valid item for the player shop icon.")
                            return@thenAccept
                        }

                        shop.icon = hand
                        save(sender.uniqueId, shop)
                        it.reply("&aPlayer shop icon has been set.")
                    }
                }
            }
            .registerAndBind(consumer, "pwarp", "pwarps", "pshops", "pwarps", "pshop")
    }
}