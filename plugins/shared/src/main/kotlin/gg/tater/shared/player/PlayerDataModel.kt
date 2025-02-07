package gg.tater.shared.player

import com.google.gson.*
import gg.tater.shared.network.model.server.ServerType
import gg.tater.shared.player.chat.color.ChatColor
import gg.tater.shared.player.position.PlayerPositionResolver
import gg.tater.shared.player.position.WrappedPosition
import gg.tater.shared.redis.Redis
import me.lucko.helper.serialize.Serializers
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type
import java.util.*

@Redis.Mapping("player_data_model")
data class PlayerDataModel(
    val uuid: UUID,
    var name: String,
    var lastServerType: ServerType,
    var currentServerId: String? = null,
    var lastPositionMap: MutableMap<ServerType, WrappedPosition> = mutableMapOf(),
    var inventoryMap: MutableMap<InventoryType, Array<out ItemStack?>> = mutableMapOf(),
    var health: Double = 20.0,
    var hunger: Int = 20,
    var exp: Float = 0F,
    var totalExp: Int = 0,
    var level: Int = 0,
    var online: Boolean = false,
    var resolver: Pair<PlayerPositionResolver.Type, String?>? = null,
    var chatColor: ChatColor? = null,
    var islandId: UUID? = null
) {

    companion object {
        const val UUID_FIELD = "uuid"
        const val NAME_FIELD = "name"
        const val POSITION_MAP_FIELD = "position_map"
        const val LAST_SERVER_TYPE_FIELD = "last_server_type"
        const val INVENTORY_MAP_FIELD = "inv_map"
        const val HEALTH_FIELD = "health"
        const val HUNGER_FIELD = "hunger"
        const val EXP_FIELD = "exp"
        const val TOTAL_EXP_FIELD = "total_exp"
        const val LEVEL_FIELD = "level"
        const val ONLINE_FIELD = "online"
        const val ACTION_FIELD = "action"
        const val CHAT_COLOR_FIELD = "chat_color"
        const val ISLAND_ID_FIELD = "island_id"
        const val CURRENT_SERVER_ID_FIELD = ""
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PlayerDataModel) return false
        return other.uuid == this.uuid
    }

    enum class InventoryType {
        ARMOR,
        REGULAR
    }

    fun setDefaultSpawn(type: ServerType) {
        lastPositionMap[type] = type.spawn
    }

    fun setSpawn(type: ServerType, position: WrappedPosition) {
        lastPositionMap[type] = position
    }

    fun getSpawn(type: ServerType): WrappedPosition {
        return lastPositionMap.getOrDefault(type, type.spawn)
    }

    fun setPositionResolver(type: PlayerPositionResolver.Type): PlayerDataModel {
        this.resolver = Pair(type, null)
        return this
    }

    fun setPositionResolver(type: PlayerPositionResolver.Type, meta: String): PlayerDataModel {
        this.resolver = Pair(type, meta)
        return this
    }

    private fun setInventory(type: InventoryType, inventory: Array<out ItemStack?>) {
        inventoryMap[type] = inventory
    }

    private fun setLastPosition(type: ServerType, position: WrappedPosition) {
        lastPositionMap[type] = position
    }

    private fun getInventory(type: InventoryType): Array<out ItemStack?>? {
        return inventoryMap[type]
    }

    fun apply(player: Player) {
        val armor = getInventory(InventoryType.ARMOR)
        if (armor != null) player.inventory.armorContents = armor

        val regular = getInventory(InventoryType.REGULAR)
        if (regular != null) player.inventory.contents = regular

        player.foodLevel = hunger
        player.health = health
        player.exp = exp
        player.totalExperience = totalExp
        player.level = level
    }

    fun update(player: Player, type: ServerType): PlayerDataModel {
        lastServerType = type
        setLastPosition(type, WrappedPosition(player.location))

        setInventory(InventoryType.ARMOR, player.inventory.armorContents)
        setInventory(InventoryType.REGULAR, player.inventory.contents)

        health = player.health
        hunger = player.foodLevel
        exp = player.exp
        totalExp = player.totalExperience
        level = player.level
        online = false

        return this
    }

    class Adapter : JsonSerializer<PlayerDataModel>, JsonDeserializer<PlayerDataModel> {
        override fun serialize(model: PlayerDataModel, type: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject()
                .apply {
                    val positionMap = JsonObject()
                    val inventoryMap = JsonObject()

                    model.lastPositionMap.forEach { (key, value) ->
                        positionMap.add(key.name, JsonObject().apply {
                            addProperty("x", value.x)
                            addProperty("y", value.y)
                            addProperty("z", value.z)
                            addProperty("yaw", value.yaw)
                            addProperty("pitch", value.pitch)
                        })
                    }

                    model.inventoryMap.forEach { (key, value) ->
                        inventoryMap.add(key.name, Serializers.serializeItemstacks(value))
                    }

                    addProperty(UUID_FIELD, model.uuid.toString())
                    addProperty(NAME_FIELD, model.name)
                    addProperty(LAST_SERVER_TYPE_FIELD, model.lastServerType.name)
                    addProperty(HEALTH_FIELD, model.health)
                    addProperty(HUNGER_FIELD, model.hunger)
                    addProperty(EXP_FIELD, model.exp)
                    addProperty(TOTAL_EXP_FIELD, model.totalExp)
                    addProperty(LEVEL_FIELD, model.level)
                    addProperty(ONLINE_FIELD, model.online)

                    add(POSITION_MAP_FIELD, positionMap)
                    add(INVENTORY_MAP_FIELD, inventoryMap)

                    model.resolver?.let {
                        add(ACTION_FIELD, JsonObject().apply {
                            addProperty("resolver", it.first.name)
                            addProperty("meta", it.second)
                        })
                    }

                    model.currentServerId?.let {
                        addProperty(CURRENT_SERVER_ID_FIELD, model.currentServerId)
                    }

                    model.chatColor?.let {
                        addProperty(CHAT_COLOR_FIELD, it.name)
                    }

                    model.islandId?.let { addProperty(ISLAND_ID_FIELD, it.toString()) }
                }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): PlayerDataModel {
            (element as JsonObject).apply {
                val lastPositionMap = mutableMapOf<ServerType, WrappedPosition>()
                val lastInventoryMap = mutableMapOf<InventoryType, Array<out ItemStack?>>()

                val positionMap = getAsJsonObject(POSITION_MAP_FIELD)
                val inventoryMap = getAsJsonObject(INVENTORY_MAP_FIELD)

                positionMap.entrySet().forEach { (key, value) ->
                    lastPositionMap[ServerType.valueOf(key)] = (value as JsonObject).let {
                        WrappedPosition(
                            it.get("x").asDouble,
                            it.get("y").asDouble,
                            it.get("z").asDouble,
                            it.get("yaw").asFloat,
                            it.get("pitch").asFloat,
                        )
                    }
                }

                inventoryMap.entrySet().forEach { (key, value) ->
                    val inventory = InventoryType.valueOf(key)
                    val stacks = Serializers.deserializeItemstacks(value as JsonPrimitive)
                    lastInventoryMap[inventory] = stacks
                }

                return PlayerDataModel(
                    UUID.fromString(get(UUID_FIELD).asString),
                    get(NAME_FIELD).asString,
                    ServerType.valueOf(get(LAST_SERVER_TYPE_FIELD).asString),
                    get(CURRENT_SERVER_ID_FIELD)?.asString,
                    lastPositionMap,
                    lastInventoryMap,
                    get(HEALTH_FIELD).asDouble,
                    get(HUNGER_FIELD).asInt,
                    get(EXP_FIELD).asFloat,
                    get(TOTAL_EXP_FIELD).asInt,
                    get(LEVEL_FIELD).asInt,
                    get(ONLINE_FIELD).asBoolean,
                    get(ACTION_FIELD)?.asJsonObject?.let {
                        Pair(
                            PlayerPositionResolver.Type.valueOf(it.get("resolver").asString),
                            it.get("meta")?.asString
                        )
                    },
                    get(CHAT_COLOR_FIELD)?.asString?.let { ChatColor.get(it) },
                    get(ISLAND_ID_FIELD)?.asString?.let { UUID.fromString(it) }
                )
            }
        }
    }
}
