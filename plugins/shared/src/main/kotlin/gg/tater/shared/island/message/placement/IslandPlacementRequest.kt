package gg.tater.shared.island.message.placement

import com.google.gson.*
import gg.tater.shared.island.Island
import gg.tater.shared.redis.Redis
import gg.tater.shared.network.model.ServerDataModel
import gg.tater.shared.network.model.ServerType
import me.lucko.helper.promise.ThreadContext
import org.bukkit.entity.Player
import java.lang.reflect.Type
import java.util.*

@Redis.Mapping("island_placement_req")
@Redis.ReqRes("island_actions")
class IslandPlacementRequest(
    val server: String,
    val playerId: UUID,
    val islandId: UUID,
    val name: String,
    var internal: Boolean = false
) {

    companion object {
        const val SERVER_FIELD = "server"
        const val PLAYER_ID_FIELD = "player_id"
        const val NAME_FIELD = "name"
        const val INTERNAL_FIELD = "login"
        const val ISLAND_ID_FIELD = "island_id"

        private fun build(sender: Player, island: Island, server: ServerDataModel): IslandPlacementRequest {
            return IslandPlacementRequest(server.id, sender.uniqueId, island.id, sender.name, true)
        }

        fun directToActive(redis: Redis, sender: Player, island: Island): Boolean {
            if (ThreadContext.forCurrentThread() != ThreadContext.ASYNC) {
                throw IllegalStateException("This method must be called asynchronously.")
            }

            // If the island is already placed on a server, teleport the player to the server
            val currentServerId = island.currentServerId
            var server: ServerDataModel?

            if (currentServerId != null) {
                server = redis.getServer(currentServerId).get()

                // If the server is not online, place the island on a fresh server
                if (server == null) {
                    server = redis.getServer(ServerType.SERVER) ?: return false
                }
            } else {
                // If everything else fails to check, place the island on a fresh server
                server = redis.getServer(ServerType.SERVER) ?: return false
            }

            redis.publish(build(sender, island, server))
            island.currentServerId = server.id
            redis.islands()[island.id] = island
            return true
        }
    }

    class Adapter : JsonSerializer<IslandPlacementRequest>, JsonDeserializer<IslandPlacementRequest> {
        override fun serialize(
            model: IslandPlacementRequest,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {
                addProperty(SERVER_FIELD, model.server)
                addProperty(PLAYER_ID_FIELD, model.playerId.toString())
                addProperty(NAME_FIELD, model.name)
                addProperty(INTERNAL_FIELD, model.internal)
                addProperty(ISLAND_ID_FIELD, model.islandId.toString())
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): IslandPlacementRequest {
            return (element as JsonObject).run {
                val server = get(SERVER_FIELD).asString
                val playerId = UUID.fromString(get(PLAYER_ID_FIELD).asString)
                val name = get(NAME_FIELD).asString
                val internal = get(INTERNAL_FIELD).asBoolean
                val islandId = UUID.fromString(get(ISLAND_ID_FIELD).asString)
                IslandPlacementRequest(server, playerId, islandId, name, internal)
            }
        }
    }
}