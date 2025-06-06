package gg.tater.core.player.kit

import gg.tater.core.player.kit.model.KitPlayerDataModel
import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface KitService : TerminableModule {

    fun compute(uuid: UUID): RFuture<KitPlayerDataModel>

    fun save(uuid: UUID, data: KitPlayerDataModel): RFuture<Boolean>

}