package gg.tater.core.server.model

import gg.tater.core.position.WrappedPosition

private val PORT_MAP: Map<ServerType, Int> = mapOf(
    ServerType.ONEBLOCK_SERVER to 25566,
    ServerType.ONEBLOCK_SPAWN to 25567,
    ServerType.HUB to 25568,
)

enum class ServerType(val spawn: WrappedPosition? = null) {

    ONEBLOCK_SERVER(WrappedPosition(0.0, 101.0, 0.0, 45F, -1.7F)),
    ONEBLOCK_SPAWN(WrappedPosition(0.574, 64.0, 0.353, 179.1F, -0.8F)),
    ONEBLOCK_PVP,
    ONEBLOCK_PLANET,

    HUB(WrappedPosition(30.538, 99.0, 124.574, 179.8F, -1.4F))
}

fun String.toServerType(): ServerType {
    val split = this.split("-")

    // If first part of server name isn't found, go to second parse
    return try {
        ServerType.valueOf(split[0].uppercase())
    } catch (e: IllegalArgumentException) {
        ServerType.valueOf("${split[0]}_${split[1]}".uppercase())
    }
}

fun ServerType.getPort(): Int? {
    return PORT_MAP[this]
}

val ONEBLOCK_GAMEMODE_SERVERS: Set<ServerType> =
    setOf(ServerType.ONEBLOCK_SERVER, ServerType.ONEBLOCK_SPAWN, ServerType.ONEBLOCK_PLANET, ServerType.ONEBLOCK_PVP)