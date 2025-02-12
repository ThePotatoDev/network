package gg.tater.shared.player.pm

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface PrivateMessageService : TerminableModule {

    companion object {
        const val MESSAGE_TARGET_MAP_NAME = "message_targets"
    }

    fun get(uuid: UUID): RFuture<UUID>

    fun set(uuid: UUID, targetId: UUID): RFuture<Boolean>
}