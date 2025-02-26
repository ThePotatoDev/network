package gg.tater.shared

import io.github.classgraph.ClassGraph
import kotlin.reflect.KClass

fun findAnnotatedClasses(annotation: KClass<out Annotation>, packages: List<String>): List<KClass<*>> {
    return ClassGraph()
        .enableAnnotationInfo()
        .acceptPackages(*packages.toTypedArray())
        .scan()
        .getClassesWithAnnotation(annotation.java)
        .loadClasses()
        .map { it.kotlin }
}

fun findAllAnnotatedClasses(annotation: KClass<out Annotation>): List<KClass<*>> {
    return ClassGraph()
        .enableAnnotationInfo()
        .scan()
        .getClassesWithAnnotation(annotation.java)
        .loadClasses()
        .map { it.kotlin }
}