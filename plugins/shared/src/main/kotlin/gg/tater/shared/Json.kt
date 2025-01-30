package gg.tater.shared

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import gg.tater.shared.island.Island
import gg.tater.shared.island.message.IslandDeleteRequest
import gg.tater.shared.island.message.IslandUpdateRequest
import gg.tater.shared.island.message.placement.IslandPlacementRequest
import gg.tater.shared.island.message.placement.IslandPlacementResponse
import gg.tater.shared.network.model.ServerDataModel
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.player.PlayerRedirectRequest
import gg.tater.shared.player.auction.AuctionHouseItem
import gg.tater.shared.player.chat.message.ChatMessageRequest
import gg.tater.shared.player.economy.PlayerEconomyModel
import gg.tater.shared.player.kit.KitPlayerDataModel
import gg.tater.shared.player.message.PlayerPrivateMessageRequest
import gg.tater.shared.player.message.PlayerPrivateMessageResponse
import gg.tater.shared.player.playershop.PlayerShopDataModel
import gg.tater.shared.player.position.WrappedPosition
import gg.tater.shared.player.vault.VaultDataModel

private val BUILDER: GsonBuilder = GsonBuilder()
    .registerTypeAdapter(Island::class.java, Island.Adapter())
    .registerTypeAdapter(PlayerDataModel::class.java, PlayerDataModel.Adapter())
    .registerTypeAdapter(ServerDataModel::class.java, ServerDataModel.Adapter())
    .registerTypeAdapter(IslandPlacementRequest::class.java, IslandPlacementRequest.Adapter())
    .registerTypeAdapter(IslandPlacementResponse::class.java, IslandPlacementResponse.Adapter())
    .registerTypeAdapter(PlayerRedirectRequest::class.java, PlayerRedirectRequest.Adapter())
    .registerTypeAdapter(PlayerPrivateMessageRequest::class.java, PlayerPrivateMessageRequest.Adapter())
    .registerTypeAdapter(PlayerPrivateMessageResponse::class.java, PlayerPrivateMessageResponse.Adapter())
    .registerTypeAdapter(IslandUpdateRequest::class.java, IslandUpdateRequest.Adapter())
    .registerTypeAdapter(IslandDeleteRequest::class.java, IslandDeleteRequest.Adapter())
    .registerTypeAdapter(ChatMessageRequest::class.java, ChatMessageRequest.Adapter())
    .registerTypeAdapter(WrappedPosition::class.java, WrappedPosition.Adapter())
    .registerTypeAdapter(PlayerEconomyModel::class.java, PlayerEconomyModel.Adapter())
    .registerTypeAdapter(KitPlayerDataModel::class.java, KitPlayerDataModel.Adapter())
    .registerTypeAdapter(AuctionHouseItem::class.java, AuctionHouseItem.Adapter())
    .registerTypeAdapter(VaultDataModel::class.java, VaultDataModel.Adapter())
    .registerTypeAdapter(PlayerShopDataModel::class.java, PlayerShopDataModel.Adapter())

val JSON: Gson = BUILDER.create()
