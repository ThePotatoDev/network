plugins {
    kotlin("kapt")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    implementation("io.kubernetes:client-java:13.0.0")
    implementation("io.kubernetes:client-java-api:13.0.0")
    implementation("net.jodah:expiringmap:0.5.11")
    implementation("org.redisson:redisson:3.36.0")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation(kotlin("reflect"))
    implementation(project(":plugins:core"))
}

tasks.shadowJar {
    archiveBaseName.set("proxy")
    archiveClassifier.set("")
    archiveVersion.set("")

    doLast {
        val jarFile = archiveFile.get().asFile
        val destinationDir = file("../../servers/proxy/src/main/docker/plugins")

        println("Moving JAR file: ${jarFile.absolutePath} to ${destinationDir.absolutePath}")

        // Ensure the destination directory exists
        destinationDir.mkdirs()

        // Move the JAR file to the destination directory
        jarFile.copyTo(destinationDir.resolve(jarFile.name), overwrite = true)

        // Delete the original JAR file
        jarFile.delete()
    }
}