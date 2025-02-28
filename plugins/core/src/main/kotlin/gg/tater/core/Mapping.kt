package gg.tater.core

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Initialize the mappings for the codec
 * Mappings are ID references to POJO's that are serialized and deserialized
 * ID's should not change once a mapping is created and actively used in production.
 * If a mapping is changed, the Redis map should be cleared to prevent deserialization errors.
 *
 * The overarching goal of the mapping system is to allow POJO classes
 * to be relocated or renamed without breaking the serialization/deserialization process.
 *
 * If a mapping does not exist for an object, the class name will be used as the mapping. (This is not recommended but required for classes such as string, int, etc.)
 */
object Mappings {

    private val mappingsByClazz: MutableMap<KClass<*>, String> = mutableMapOf()
    private val mappingsById: MutableMap<String, KClass<*>> = mutableMapOf()

    fun loadMappings() {
        for (clazz in findAnnotatedClasses(Mapping::class)) {
            val mapping = clazz.findAnnotation<Mapping>() ?: continue
            mappingsById[mapping.id] = clazz
            mappingsByClazz[clazz] = mapping.id
        }
    }

    fun getMappingByClazz(clazz: KClass<*>): String? {
        return mappingsByClazz[clazz]
    }

    fun getMappingById(id: String): KClass<*>? {
        return mappingsById[id]
    }

    fun computeById(id: String): KClass<*> {
        return mappingsById.computeIfAbsent(id) {
            Class.forName(id).kotlin
        }
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Mapping(val id: String)