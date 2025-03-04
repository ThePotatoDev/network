package gg.tater.oneblock.island

import com.google.gson.*
import com.infernalsuite.aswm.api.world.SlimeWorld
import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.model.ModeledEntity
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.Npc
import de.oliver.fancynpcs.api.NpcData
import gg.tater.core.JsonAdapter
import gg.tater.core.Mapping
import gg.tater.core.island.Island
import gg.tater.core.position.WrappedPosition
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import java.lang.reflect.Type
import java.util.*

@Mapping("oneblock_islands")
class OneBlockIsland(id: UUID, ownerId: UUID, ownerName: String, var level: Int = 1, var ftue: Boolean = true) : Island(
    id,
    ownerId,
    ownerName,
    spawn = WrappedPosition(0.5, 70.0, 0.5, 0F, 0F)
) {

    private var shipEntity: ModeledEntity? = null
    private var npcEntity: Npc? = null

    companion object {
        // Breakable block location is a block below the default spawn for OneBlock islands
        val ONE_BLOCK_LOCATION = WrappedPosition(0.0, 69.0, 0.0, 0F, 0F)
        val SPACE_SHIP_LOCATION = WrappedPosition(0.464, 70.0, 41.546, 156F, -0.8F)
        val SPACE_SHIP_NPC_LOCATION = WrappedPosition(4.513, 70.0, 41.401, 156F, -0.8F)

        private const val LEVEL_FIELD = "island_level"
        private const val FTUE_FIELD = "ftue"

        private val NPC_INSTANCE = FancyNpcsPlugin.get()
    }

    fun deleteBrokenShip() {
        shipEntity?.destroy()
    }

    fun spawnBrokenShip(slimeWorld: SlimeWorld) {
        val world = Bukkit.getWorld(slimeWorld.name)!!

        val entity = ModelEngineAPI.createModeledEntity(
            world.spawn(
                Location(
                    world,
                    SPACE_SHIP_LOCATION.x,
                    SPACE_SHIP_LOCATION.y,
                    SPACE_SHIP_LOCATION.z,
                    SPACE_SHIP_LOCATION.yaw,
                    SPACE_SHIP_LOCATION.pitch
                ),
                ArmorStand::class.java
            ).apply {
                isVisible = false
            }
        )

        entity.addModel(ModelEngineAPI.createActiveModel("ship_broken"), false)
        shipEntity = entity
    }

    fun spawnShipNPC(slimeWorld: SlimeWorld) {
        val world = Bukkit.getWorld(slimeWorld.name)!!

        val data = NpcData(
            "Astronaut",
            UUID.randomUUID(),
            Location(
                world,
                SPACE_SHIP_NPC_LOCATION.x,
                SPACE_SHIP_NPC_LOCATION.y,
                SPACE_SHIP_NPC_LOCATION.z,
                SPACE_SHIP_NPC_LOCATION.yaw,
                SPACE_SHIP_NPC_LOCATION.pitch
            )
        )

        data.displayName = "<aqua>Astronaut"

        val npc = NPC_INSTANCE.npcAdapter.apply(data)
        this.npcEntity = npc

        NPC_INSTANCE.npcManager.registerNpc(npc)

        npc.create()
        npc.spawnForAll()
    }

    fun deleteShipNPC() {
        this.npcEntity!!.removeForAll()
        NPC_INSTANCE.npcManager.removeNpc(this.npcEntity)
    }

    @JsonAdapter(OneBlockIsland::class)
    class Adapter : JsonSerializer<OneBlockIsland>, JsonDeserializer<OneBlockIsland> {
        private val baseAdapter = Island.Adapter()

        override fun serialize(island: OneBlockIsland, type: Type, context: JsonSerializationContext): JsonElement {
            return (baseAdapter.serialize(island, type, context) as JsonObject).apply {
                addProperty(LEVEL_FIELD, island.level)
                addProperty(FTUE_FIELD, island.ftue)
            }
        }

        override fun deserialize(
            element: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): OneBlockIsland {
            val island = baseAdapter.deserialize(element, type, context)
            return (element as JsonObject).let {
                val level = it.get(LEVEL_FIELD).asInt
                val ftue = it.get(FTUE_FIELD).asBoolean
                OneBlockIsland(island.id, island.ownerId, island.ownerName, level, ftue)
            }
        }
    }
}