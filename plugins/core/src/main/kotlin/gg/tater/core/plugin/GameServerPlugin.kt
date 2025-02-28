package gg.tater.core.plugin

import me.lucko.helper.plugin.ExtendedJavaPlugin
import me.lucko.helper.terminable.module.TerminableModule

abstract class GameServerPlugin : ExtendedJavaPlugin() {

    fun <T : TerminableModule> useController(instance: T) {
        bindModule(instance)
    }
}