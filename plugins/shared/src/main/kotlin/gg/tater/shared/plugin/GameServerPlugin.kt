package gg.tater.shared.plugin

import gg.tater.shared.annotation.Controller
import gg.tater.shared.findAnnotatedClasses
import gg.tater.shared.server.model.ServerType
import me.lucko.helper.Helper
import me.lucko.helper.plugin.ExtendedJavaPlugin
import me.lucko.helper.terminable.module.TerminableModule
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

abstract class GameServerPlugin : ExtendedJavaPlugin() {

    fun initControllers(serverType: ServerType) {
        for (clazz in findAnnotatedClasses(Controller::class)) {
            val meta = clazz.findAnnotation<Controller>() ?: continue
            if (meta.ignoredBinds.contains(serverType)) continue

            if (meta.requiredPlugins.isNotEmpty() && meta.requiredPlugins.any { plugin ->
                    !Helper.plugins().isPluginEnabled(plugin)
                }) {
                logger.info("Could not enable controller: ${clazz.simpleName}. Required plugins not present.")
                return
            }

            bindModule(clazz.primaryConstructor?.call() as TerminableModule)
            logger.info("Bound controller as module: ${clazz.simpleName}")
        }
    }
}