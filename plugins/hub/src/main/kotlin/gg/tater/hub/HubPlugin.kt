package gg.tater.hub

import gg.tater.core.plugin.GameServerPlugin

class HubPlugin: GameServerPlugin() {

    override fun enable() {
        useController(HubController())
    }
}