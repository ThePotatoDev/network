package gg.tater.shared.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Controller(val id: String, val requiredPlugins: Array<String> = [])
