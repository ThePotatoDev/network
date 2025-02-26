import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.lucko:helper:5.6.14")
    api(project(":plugins:shared"))
}

tasks.shadowJar {
    archiveBaseName.set("hub")
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
    main = "gg.tater.hub.HubPlugin"
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
    }
}