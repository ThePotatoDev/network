subprojects {
    tasks.shadowJar {
        dependsOn(tasks.compileJava)
        archiveBaseName.set(project.name)
    }

    tasks.build {
        dependsOn(tasks.shadowJar)
    }
}


