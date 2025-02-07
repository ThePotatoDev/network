package gg.tater.shared

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.reflect.KClass

object Json {
    private val BUILDER: GsonBuilder = GsonBuilder()

    init {
        for (clazz in findAnnotatedClasses(JsonAdapter::class.java)) {
            val meta = clazz.getAnnotation(JsonAdapter::class.java)
            BUILDER.registerTypeAdapter(meta.target.java, clazz.getDeclaredConstructor().newInstance())
        }
    }

    val INSTANCE: Gson = BUILDER.create()
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class JsonAdapter(val target: KClass<*>)