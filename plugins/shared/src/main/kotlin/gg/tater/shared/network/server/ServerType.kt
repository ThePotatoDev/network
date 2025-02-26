package gg.tater.shared.network.server

import gg.tater.shared.player.position.WrappedPosition

enum class ServerType(val spawn: WrappedPosition? = null) {

    SERVER(WrappedPosition(0.0, 101.0, 0.0, 45F, -1.7F)),
    SPAWN(WrappedPosition(0.574, 64.0, 0.353, 179.1F, -0.8F)),
    HUB(),
    PVP,
    PLANET,
    ALL

}

fun getServerTypeFromId(id: String): ServerType {
    val split = id.split("-")[0]
    return ServerType.valueOf(split.uppercase())
}