package gg.tater.shared.annotation

import gg.tater.shared.server.model.ServerType

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Controller(
    val id: String,
    val ignoredBinds: Array<ServerType> = [],
    val requiredPlugins: Array<String> = []
)
