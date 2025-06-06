package gg.tater.core.position

import com.google.gson.*
import gg.tater.core.JsonAdapter
import gg.tater.core.Mapping
import org.bukkit.Location
import java.lang.reflect.Type

@Mapping("wrapped_position")
data class WrappedPosition(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
) {

    constructor(location: Location) : this(location.x, location.y, location.z, location.yaw, location.pitch)

    constructor(position: WrappedPosition) : this(
        position.x,
        position.y,
        position.z,
        position.yaw,
        position.pitch
    )

    @JsonAdapter(WrappedPosition::class)
    class Adapter : JsonSerializer<WrappedPosition>, JsonDeserializer<WrappedPosition> {
        override fun serialize(
            src: WrappedPosition,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {
                addProperty("x", src.x)
                addProperty("y", src.y)
                addProperty("z", src.z)
                addProperty("yaw", src.yaw)
                addProperty("pitch", src.pitch)
            }
        }

        override fun deserialize(
            element: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): WrappedPosition {
            (element as JsonObject).apply {
                return WrappedPosition(
                    get("x").asDouble,
                    get("y").asDouble,
                    get("z").asDouble,
                    get("yaw").asFloat,
                    get("pitch").asFloat,
                )
            }
        }
    }
}