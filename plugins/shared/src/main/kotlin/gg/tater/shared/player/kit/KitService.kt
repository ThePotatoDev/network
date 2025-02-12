package gg.tater.shared.player.kit

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface KitService : TerminableModule {

    fun compute(uuid: UUID): RFuture<KitPlayerDataModel>

    fun save(uuid: UUID, data: KitPlayerDataModel): RFuture<Boolean>

}