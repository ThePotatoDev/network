import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
}

dependencies {
    val scoreboardLibraryVersion = "2.2.1"

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.lucko:helper:5.6.14")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("net.megavex:scoreboard-library-api:$scoreboardLibraryVersion")

    implementation(kotlin("reflect"))
    implementation("org.redisson:redisson:3.36.0")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation("io.github.classgraph:classgraph:4.8.179")
    implementation("net.megavex:scoreboard-library-implementation:$scoreboardLibraryVersion")
    implementation("net.megavex:scoreboard-library-modern:$scoreboardLibraryVersion:mojmap")

    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

tasks.shadowJar {
    archiveBaseName.set("core")
    archiveClassifier.set("")
    archiveVersion.set("")

    doLast {
        val jarFile = archiveFile.get().asFile

        val destinationDirs = listOf(
            file("../../servers/server/src/main/docker/plugins"),
            file("../../servers/spawn/src/main/docker/plugins"),
            file("../../servers/hub/src/main/docker/plugins")
        )

        destinationDirs.forEach { destinationDir ->
            println("Moving JAR file: ${jarFile.absolutePath} to ${destinationDir.absolutePath}")

            // Ensure the destination directory exists
            destinationDir.mkdirs()

            // Move the JAR file to the destination directory
            jarFile.copyTo(destinationDir.resolve(jarFile.name), overwrite = true)
        }

        // Delete the original JAR file
        jarFile.delete()
    }
}

paper {
    main = "gg.tater.core.CorePlugin"
    foliaSupported = false
    apiVersion = "1.20"
    serverDependencies {
        register("LuckPerms") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }

        register("helper") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }

        register("SlimeWorldPlugin") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }

        register("FancyNpcs") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }
    }
}