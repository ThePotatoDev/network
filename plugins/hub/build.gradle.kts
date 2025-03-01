import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.lucko:helper:5.6.14")
    compileOnly("org.redisson:redisson:3.36.0")
    compileOnly(project(":plugins:core"))
    compileOnly(project(":plugins:oneblock-core"))
}

tasks.shadowJar {
    archiveBaseName.set("hub")
    archiveClassifier.set("")
    archiveVersion.set("")

    doLast {
        val jarFile = archiveFile.get().asFile

        val destinationDirs = listOf(
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
        register("core") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = true
        }

        register("LuckPerms") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }

        register("helper") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}