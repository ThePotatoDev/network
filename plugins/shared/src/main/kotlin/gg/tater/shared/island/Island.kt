package gg.tater.shared.island

import com.google.gson.*
import gg.tater.shared.Json
import gg.tater.shared.JsonAdapter
import gg.tater.shared.annotation.Mapping
import gg.tater.shared.island.flag.model.FlagType
import gg.tater.shared.island.setting.model.IslandSettingType
import gg.tater.shared.network.server.ServerType
import gg.tater.shared.player.position.WrappedPosition
import gg.tater.shared.redis.Redis
import java.lang.reflect.Type
import java.time.Instant
import java.util.*

@Mapping("islands")
class Island(
    val id: UUID,
    val ownerId: UUID,
    val ownerName: String,
    var members: MutableMap<UUID, Role> = mutableMapOf(ownerId to Role.OWNER),
    var flags: MutableMap<FlagType, Role> = mutableMapOf(),
    var settings: MutableMap<IslandSettingType, Boolean> = mutableMapOf(),
    var warps: MutableMap<String, WrappedPosition> = mutableMapOf(),
    var currentServerId: String? = null,
    var lastActivity: Instant = Instant.now(),
    var spawn: WrappedPosition = WrappedPosition(ServerType.SERVER.spawn!!),
    var level: Int = 1
) {

    private companion object {
        const val ID_FIELD = "id"
        const val OWNER_UUID_FIELD = "uuid"
        const val OWNER_NAME_FIELD = "name"
        const val MEMBER_FIELD = "members"
        const val FLAG_FIELD = "flags"
        const val CURRENT_SERVER_ID_FIELD = "current_server_id"
        const val LAST_ACTIVITY_FIELD = "last_activity"
        const val SPAWN_FIELD = "spawn"
        const val SETTINGS_FIELD = "settings"
        const val WARPS_FIELD = "warps"
        const val LEVEL_FIELD = "level"
    }

    enum class Role(val hierarchy: Int, val friendly: String) {
        OWNER(4, "Owner"),
        CO_OWNER(3, "Co-Owner"),
        MEMBER(2, "Member"),
        VISITOR(1, "Visitor")
        ;

        fun next(): Role {
            return entries[(this.ordinal + 1) % entries.size]
        }
    }

    fun getRoleFor(uuid: UUID): Role {
        if (uuid == ownerId) return Role.OWNER
        return members.getOrDefault(uuid, Role.VISITOR)
    }

    fun setRoleFor(uuid: UUID, role: Role) {
        if (uuid == ownerId) return
        members[uuid] = role
    }

    fun canInteract(uuid: UUID, flag: FlagType): Boolean {
        val role = getRoleFor(uuid)
        return role.hierarchy >= flags.getOrDefault(flag, flag.defaultRole).hierarchy
    }

    fun allMembers(): Set<UUID> {
        return members.keys
    }

    fun updateSetting(setting: IslandSettingType, value: Boolean) {
        settings[setting] = value
    }

    fun getSettingValue(setting: IslandSettingType): Boolean {
        return settings.getOrDefault(setting, setting.default)
    }

    fun isSettingEnabled(setting: IslandSettingType): Boolean {
        return settings[setting] == true
    }

    fun getFlagRole(flag: FlagType): Role {
        return flags.getOrDefault(flag, flag.defaultRole)
    }

    fun setFlagRole(flag: FlagType, role: Role) {
        flags[flag] = role
    }

    override fun equals(other: Any?): Boolean {
        return id == (other as Island).id
    }

    @JsonAdapter(Island::class)
    class Adapter : JsonSerializer<Island>, JsonDeserializer<Island> {
        override fun serialize(
            model: Island,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {

                addProperty(ID_FIELD, model.id.toString())
                addProperty(OWNER_UUID_FIELD, model.ownerId.toString())
                addProperty(OWNER_NAME_FIELD, model.ownerName)
                addProperty(LAST_ACTIVITY_FIELD, model.lastActivity.toEpochMilli())
                addProperty(SPAWN_FIELD, Json.INSTANCE.toJson(model.spawn))
                addProperty(LEVEL_FIELD, model.level)

                model.currentServerId?.let { addProperty(CURRENT_SERVER_ID_FIELD, it) }

                add(MEMBER_FIELD, JsonArray().apply {
                    model.members.forEach { (uuid, role) ->
                        add(JsonObject().apply {
                            addProperty("uuid", uuid.toString())
                            addProperty("role", role.name)
                        })
                    }
                })

                add(WARPS_FIELD, JsonArray().apply {
                    model.warps.forEach { (name, pos) ->
                        add(JsonObject().apply {
                            addProperty("name", name)
                            addProperty("pos", Json.INSTANCE.toJson(pos))
                        })
                    }
                })

                add(FLAG_FIELD, JsonArray().apply {
                    model.flags.forEach { (flag, role) ->
                        add(JsonObject().apply {
                            addProperty("flag", flag.name)
                            addProperty("role", role.name)
                        })
                    }
                })

                add(SETTINGS_FIELD, JsonArray().apply {
                    model.settings.forEach { (setting, value) ->
                        add(JsonObject().apply {
                            addProperty("setting", setting.name)
                            addProperty("value", value)
                        })
                    }
                })
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): Island {
            return (element as JsonObject).run {
                val id = UUID.fromString(get(ID_FIELD).asString)
                val ownerId = UUID.fromString(get(OWNER_UUID_FIELD).asString)
                val ownerName = get(OWNER_NAME_FIELD).asString
                val lastActivity = Instant.ofEpochMilli(get(LAST_ACTIVITY_FIELD).asLong)
                val spawn = Json.INSTANCE.fromJson(get(SPAWN_FIELD).asString, WrappedPosition::class.java)
                val currentServer = get(CURRENT_SERVER_ID_FIELD)?.asString
                val level = get(LEVEL_FIELD).asInt

                val members = mutableMapOf<UUID, Role>()
                val flags = mutableMapOf<FlagType, Role>()
                val settings = mutableMapOf<IslandSettingType, Boolean>()
                val warps = mutableMapOf<String, WrappedPosition>()

                get(MEMBER_FIELD).asJsonArray.forEach {
                    val obj = it.asJsonObject
                    val uuid = UUID.fromString(obj.get("uuid").asString)
                    val role = Role.valueOf(obj.get("role").asString)
                    members[uuid] = role
                }

                get(FLAG_FIELD).asJsonArray.forEach {
                    val obj = it.asJsonObject
                    val flag = FlagType.valueOf(obj.get("flag").asString)
                    val role = Role.valueOf(obj.get("role").asString)
                    flags[flag] = role
                }

                get(SETTINGS_FIELD).asJsonArray.forEach {
                    val obj = it.asJsonObject
                    val setting = IslandSettingType.valueOf(obj.get("setting").asString)
                    val value = obj.get("value").asBoolean
                    settings[setting] = value
                }

                get(WARPS_FIELD).asJsonArray.forEach {
                    val obj = it.asJsonObject
                    val name = obj.get("name").asString
                    val pos = Json.INSTANCE.fromJson(obj.get("pos").asString, WrappedPosition::class.java)
                    warps[name] = pos
                }

                Island(
                    id,
                    ownerId,
                    ownerName,
                    members,
                    flags,
                    settings,
                    warps,
                    currentServer,
                    lastActivity,
                    spawn,
                    level
                )
            }
        }
    }
}