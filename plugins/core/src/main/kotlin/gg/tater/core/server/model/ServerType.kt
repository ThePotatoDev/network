package gg.tater.core.server.model

import gg.tater.core.position.WrappedPosition

enum class ServerType(val spawn: WrappedPosition? = null) {

    ONEBLOCK_SERVER(WrappedPosition(0.0, 101.0, 0.0, 45F, -1.7F)),
    ONEBLOCK_SPAWN(WrappedPosition(0.574, 64.0, 0.353, 179.1F, -0.8F)),
    ONEBLOCK_PVP,
    ONEBLOCK_PLANET,

    HUB(WrappedPosition(30.538, 99.0, 124.574, 179.8F, -1.4F))
}

fun String.toServerType(): ServerType {
    val split = this.split("-")[0]
    return ServerType.valueOf(split)
}

val ONEBLOCK_GAMEMODE_SERVERS: Set<ServerType> =
    setOf(ServerType.ONEBLOCK_SERVER, ServerType.ONEBLOCK_SPAWN, ServerType.ONEBLOCK_PLANET, ServerType.ONEBLOCK_PVP)