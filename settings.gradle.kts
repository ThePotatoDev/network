plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "network"

include("plugins")
include("plugins:proxy")
findProject("plugins:proxy")?.name = "proxy"

include("servers:server")
include("servers:spawn")
include("servers:proxy")
include("servers")
include("servers:hub")
findProject(":servers:hub")?.name = "hub"
include("plugins:hub")
findProject(":plugins:hub")?.name = "hub"
include("plugins:core")
findProject(":plugins:core")?.name = "core"
include("plugins:oneblock-core")
findProject(":plugins:oneblock-core")?.name = "oneblock-core"
