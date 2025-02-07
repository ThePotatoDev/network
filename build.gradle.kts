import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "2.0.0"
    `maven-publish`
}

tasks.register<Exec>("runShellScript") {
    commandLine("sh", "${project.rootDir}/scripts/deploy_local.sh", "${project.rootDir}")
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = "gg.tater"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.fancyplugins.de/releases")
    }

    dependencies {
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        compileOnly("com.google.code.gson:gson:2.8.9") // Use the latest version
    }

    publishing {
        publications {
            create<MavenPublication>("shadow") {
                project.extensions.configure<ShadowExtension> {
                    component(this@create)
                }
            }
        }
    }

    kotlin {
        jvmToolchain(17)
    }
}

subprojects {
    tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        if (project.hasProperty("reset")) {
            finalizedBy(rootProject.tasks.named("runShellScript"))
        }
    }
}

tasks.named("build") {
    dependsOn(subprojects.flatMap { it.tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>() })
}