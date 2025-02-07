package gg.tater.shared

import io.github.classgraph.ClassGraph

fun findAnnotatedClasses(annotation: Class<out Annotation>): List<Class<*>> {
    return ClassGraph()
        .enableAnnotationInfo()
        .acceptPackages("gg.tater")
        .scan()
        .getClassesWithAnnotation(annotation.name)
        .loadClasses()
}