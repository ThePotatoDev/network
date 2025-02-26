package gg.tater.shared.network.server

import gg.tater.shared.player.position.WrappedPosition

enum class ServerType(val spawn: WrappedPosition? = null) {

    SERVER(WrappedPosition(0.0, 101.0, 0.0, 45F, -1.7F)),
    SPAWN(WrappedPosition(0.574, 64.0, 0.353, 179.1F, -0.8F)),
    PVP,
    PLANET,
    HUB(WrappedPosition(30.538, 99.0, 124.574, 179.8F, -1.4F))

}

fun String.toServerType(): ServerType {
    val split = this.split("-")[0]
    return ServerType.valueOf(split.uppercase())
}

val ONEBLOCK_GAMEMODE_SERVERS: Set<ServerType> =
    setOf(ServerType.SERVER, ServerType.SPAWN, ServerType.PLANET, ServerType.PVP)