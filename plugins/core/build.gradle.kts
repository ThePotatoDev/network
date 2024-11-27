import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
}

repositories {
    flatDir {
        dirs("libs")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.lucko:helper:5.6.14")
    compileOnly("net.luckperms:api:5.4")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.redisson:redisson:3.36.0")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation(kotlin("reflect"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api(project(":plugins:shared"))

    val scoreboardLibraryVersion = "2.2.1"
    implementation("net.megavex:scoreboard-library-api:$scoreboardLibraryVersion")
    runtimeOnly("net.megavex:scoreboard-library-implementation:$scoreboardLibraryVersion")
    runtimeOnly("net.megavex:scoreboard-library-modern:$scoreboardLibraryVersion:mojmap")
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
            file("../../servers/limbo/src/main/docker/plugins")
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
    }
}