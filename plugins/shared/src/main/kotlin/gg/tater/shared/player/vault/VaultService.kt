package gg.tater.shared.player.vault

import me.lucko.helper.terminable.module.TerminableModule
import org.redisson.api.RFuture
import java.util.*

interface VaultService : TerminableModule {

    fun compute(uuid: UUID): RFuture<VaultDataModel>

    fun save(uuid: UUID, vault: VaultDataModel): RFuture<VaultDataModel>

    fun get(uuid: UUID): RFuture<VaultDataModel>
}