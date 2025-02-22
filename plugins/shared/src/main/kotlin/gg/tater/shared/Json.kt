package gg.tater.shared

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import gg.tater.shared.annotation.Mapping
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

object Json {
    private val BUILDER: GsonBuilder = GsonBuilder()

    init {
        for (clazz in findAnnotatedClasses(JsonAdapter::class)) {
            val meta = clazz.findAnnotation<JsonAdapter>() ?: continue

            if (meta.target.findAnnotation<Mapping>() == null) {
                println("Class ${meta.target.simpleName} must be annotated with @Mapping before registering it with @JsonAdapter " +
                        "if you do not want to lose the reference!")
                continue
            }

            BUILDER.registerTypeAdapter(meta.target.java, clazz.primaryConstructor?.call())
        }
    }

    val INSTANCE: Gson = BUILDER.create()
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class JsonAdapter(val target: KClass<*>)