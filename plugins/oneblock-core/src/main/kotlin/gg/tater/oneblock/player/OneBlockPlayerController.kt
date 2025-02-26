package gg.tater.oneblock.player

import gg.tater.oneblock.player.model.OneBlockPlayer
import gg.tater.shared.island.player.IslandPlayerService
import gg.tater.shared.redis.Redis
import me.lucko.helper.Services
import me.lucko.helper.terminable.TerminableConsumer
import org.redisson.api.RFuture
import java.util.*

class OneBlockPlayerController : IslandPlayerService<OneBlockPlayer> {

    private companion object {
        const val PLAYER_MAP_NAME = "oneblock_players"
    }

    private val redis = Services.load(Redis::class.java)

    override fun compute(name: String, uuid: UUID): RFuture<OneBlockPlayer> {
        TODO("Not yet implemented")
    }

    override fun get(uuid: UUID): RFuture<OneBlockPlayer> {
        TODO("Not yet implemented")
    }

    override fun setup(p0: TerminableConsumer) {
        TODO("Not yet implemented")
    }

    override fun transaction(data: OneBlockPlayer, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun save(data: OneBlockPlayer): RFuture<Boolean> {
        TODO("Not yet implemented")
    }
}