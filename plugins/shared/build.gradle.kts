dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("org.redisson:redisson:3.36.0")
    compileOnly("de.oliver:FancyNpcs:2.4.2")
    compileOnly("net.luckperms:api:5.4")
    compileOnly(kotlin("reflect"))
    compileOnly("me.lucko:helper:5.6.14")
    compileOnly("de.oliver:FancyHolograms:2.4.2")
    implementation("io.github.classgraph:classgraph:4.8.179")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    val scoreboardLibraryVersion = "2.2.1"
    compileOnly("net.megavex:scoreboard-library-api:$scoreboardLibraryVersion")
    runtimeOnly("net.megavex:scoreboard-library-implementation:$scoreboardLibraryVersion")
    runtimeOnly("net.megavex:scoreboard-library-modern:$scoreboardLibraryVersion:mojmap")
}