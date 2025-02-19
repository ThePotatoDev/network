package gg.tater.core.controllers.player.combat

import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.Npc
import de.oliver.fancynpcs.api.NpcData
import de.oliver.fancynpcs.api.utils.SkinFetcher
import gg.tater.shared.annotation.Controller
import gg.tater.shared.player.combat.CombatService
import gg.tater.shared.player.combat.model.CombatLogEntry
import gg.tater.shared.redis.Redis
import me.lucko.helper.Events
import me.lucko.helper.Services
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.TerminableConsumer
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.TimeUnit

// References SkinFetcher (Will remove when FancyNPC's updates)
@Suppress("DEPRECATION")
@Controller(
    id = "combat-controller",
    requiredPlugins = ["FancyNpcs"]
)
class CombatController : CombatService {

    private val redis = Services.load(Redis::class.java)

    //TODO: handle rejoins & inventory item pulling synchronization

    private companion object {
        val PLUGIN: FancyNpcsPlugin = FancyNpcsPlugin.get()
    }

    private val combat: MutableSet<UUID> = Collections.newSetFromMap(
        ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.CREATED)
            .expiration(15L, TimeUnit.SECONDS)
            .build()
    )

    private val loggers: MutableMap<String, CombatLogEntry> = ExpiringMap.builder()
        .expirationPolicy(ExpirationPolicy.CREATED)
        .expiration(15L, TimeUnit.SECONDS)
        .asyncExpirationListener { id: String, _: CombatLogEntry ->
            val npc = PLUGIN.npcManager.getNpcById(id) ?: return@asyncExpirationListener
            npc.removeForAll()
            PLUGIN.npcManager.removeNpc(npc)
        }
        .build()

    override fun setup(consumer: TerminableConsumer) {
        Services.provide(CombatService::class.java, this)

        Events.subscribe(PlayerQuitEvent::class.java, EventPriority.HIGHEST)
            .handler {
                val player = it.player
                if (!isInCombat(player.uniqueId)) return@handler
                val npc = spawnCombatNPC(player)
                loggers[npc.data.id] = CombatLogEntry(player)
            }
            .bindWith(consumer)

        Events.subscribe(EntityDamageByEntityEvent::class.java, EventPriority.HIGHEST)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.entityType == EntityType.PLAYER }
            .handler {
                val entity = it.entity
                val damager = it.damager

                if (damager is Player) {
                    setInCombat(entity.uniqueId)
                    setInCombat(damager.uniqueId)
                    return@handler
                }

                // Check if projectile is shot by a player, then
                // enter them both into combat
                if (damager is Projectile) {
                    if (damager.shooter !is Player) return@handler
                    setInCombat((damager.shooter as Player).uniqueId)
                    setInCombat(entity.uniqueId)
                }
            }
            .bindWith(consumer)
    }

    override fun isInCombat(uuid: UUID): Boolean {
        return combat.contains(uuid)
    }

    override fun setInCombat(uuid: UUID): Boolean {
        return combat.add(uuid)
    }

    override fun spawnCombatNPC(player: Player): Npc {
        val npc =
            PLUGIN.npcAdapter.apply(NpcData(player.name, UUID.randomUUID(), player.location).apply {
                val skin = SkinFetcher.SkinData(player.name, null, null)
                setSkin(skin)
                location = player.location
                displayName = "<red><bold>${player.name}'s Combat Log"
            })

        PLUGIN.npcManager.registerNpc(npc)
        npc.create()
        npc.spawnForAll()

        return npc
    }
}