package gg.tater.core.server

import gg.tater.core.server.model.ServerType
import gg.tater.core.server.model.toServerType

interface ServerDataService {

    fun id(): String

    fun serverType(): ServerType = id().toServerType()

}