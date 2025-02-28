package gg.tater.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

object Json {
    private val BUILDER: GsonBuilder = GsonBuilder()
    private val registeredAdapters = mutableSetOf<Pair<KClass<*>, Any>>()

    @Volatile
    private var instance: Gson? = null

    fun registerAdapters() {
        for (clazz in findAnnotatedClasses(JsonAdapter::class)) {
            val meta = clazz.findAnnotation<JsonAdapter>() ?: continue
            if (meta.target.findAnnotation<Mapping>() == null) {
                println(
                    "Class ${meta.target.simpleName} must be annotated with @Mapping before registering it with @JsonAdapter " +
                            "if you do not want to lose the reference!"
                )
                continue
            }

            val adapterInstance = clazz.primaryConstructor?.call() ?: continue
            if (registeredAdapters.add(meta.target to adapterInstance)) {
                BUILDER.registerTypeAdapter(meta.target.java, adapterInstance)
                instance = null // Invalidate instance to force recreation
            }
        }
    }

    fun get(): Gson {
        return instance ?: synchronized(this) {
            instance ?: BUILDER.create().also { instance = it }
        }
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class JsonAdapter(val target: KClass<*>)