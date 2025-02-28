package gg.tater.core

import io.github.classgraph.ClassGraph
import kotlin.reflect.KClass

fun findAnnotatedClasses(annotation: KClass<out Annotation>): List<KClass<*>> {
    return ClassGraph()
        .enableAnnotationInfo()
        .acceptPackages("gg.tater")
        .scan()
        .getClassesWithAnnotation(annotation.java)
        .loadClasses()
        .map { it.kotlin }
}