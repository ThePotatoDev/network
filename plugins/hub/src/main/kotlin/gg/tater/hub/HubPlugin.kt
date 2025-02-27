package gg.tater.hub

import gg.tater.shared.plugin.GameServerPlugin

class HubPlugin: GameServerPlugin() {

    override fun enable() {
        useController(null, HubController::class)
    }
}