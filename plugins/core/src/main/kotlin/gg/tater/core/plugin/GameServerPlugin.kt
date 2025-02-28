package gg.tater.core.plugin

import gg.tater.core.annotation.Controller
import gg.tater.core.server.model.GameModeType
import me.lucko.helper.Helper
import me.lucko.helper.plugin.ExtendedJavaPlugin
import me.lucko.helper.terminable.module.TerminableModule
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

abstract class GameServerPlugin : ExtendedJavaPlugin() {

    fun useController(mode: GameModeType?, vararg controllers: KClass<*>) {
        for (clazz in controllers) {
            val meta = clazz.findAnnotation<Controller>() ?: continue
            if (meta.requiredPlugins.isNotEmpty() && meta.requiredPlugins.any { plugin ->
                    !Helper.plugins().isPluginEnabled(plugin)
                }) {
                logger.info("Could not enable controller: ${clazz.simpleName}. Required plugins not present.")
                return
            }

            val constructor = clazz.primaryConstructor!!
            val instance = (if (constructor.parameters.size == 1) {
                constructor.call(mode)
            } else {
                constructor.call()
            }) as TerminableModule

            bindModule(instance)
            logger.info("Bound controller as module: ${clazz.simpleName}")
        }
    }
}