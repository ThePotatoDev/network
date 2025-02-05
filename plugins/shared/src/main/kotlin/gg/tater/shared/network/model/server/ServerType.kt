package gg.tater.shared.network.model.server

import gg.tater.shared.player.position.WrappedPosition

enum class ServerType(val spawn: WrappedPosition) {

    SERVER(WrappedPosition(0.0, 101.0, 0.0, 45F, -1.7F)),
    SPAWN(WrappedPosition(-8.0, 67.0, 13.0, 45F, -1.7F)),
    LIMBO(WrappedPosition(0.0, 1.0, 0.0, 45F, -1.7F))

}