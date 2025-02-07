package gg.tater.shared

import io.github.classgraph.ClassGraph

fun findAnnotatedClasses(annotation: Class<out Annotation>): List<Class<*>> {
    return ClassGraph()
        .enableAllInfo()
        .scan()
        .getClassesWithAnnotation(annotation.name)
        .loadClasses()
}