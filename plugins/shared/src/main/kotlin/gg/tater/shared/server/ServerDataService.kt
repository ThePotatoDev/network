package gg.tater.shared.server

import gg.tater.shared.server.model.ServerType
import gg.tater.shared.server.model.toServerType

interface ServerDataService {

    fun id(): String

    fun serverType(): ServerType = id().toServerType()

}