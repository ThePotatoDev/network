package gg.tater.shared.redis

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.tater.shared.Json
import gg.tater.shared.findAnnotatedClasses
import gg.tater.shared.island.Island
import gg.tater.shared.network.model.ProxyDataModel
import gg.tater.shared.network.model.server.ServerDataModel
import gg.tater.shared.network.model.server.ServerState
import gg.tater.shared.network.model.server.ServerType
import gg.tater.shared.player.PlayerDataModel
import gg.tater.shared.player.auction.AuctionHouseItem
import gg.tater.shared.player.economy.PlayerEconomyModel
import gg.tater.shared.player.kit.KitPlayerDataModel
import gg.tater.shared.player.playershop.PlayerShopDataModel
import gg.tater.shared.player.vault.VaultDataModel
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import me.lucko.helper.promise.ThreadContext
import org.redisson.Redisson
import org.redisson.api.*
import org.redisson.client.codec.BaseCodec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import org.redisson.config.Config
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.reflect.KClass

class Redis(credential: Credential) {

    companion object {
        const val PLAYER_MAP_NAME = "players"
        const val SERVER_MAP_NAME = "servers"
        const val ECONOMY_MAP_NAME = "economy"
        const val MESSAGE_TARGET_MAP_NAME = "message_targets"
        const val ISLAND_MAP_NAME = "islands"
        const val INVITES_FOR_MAP_NAME = "invites_for"
        const val KIT_PLAYER_DATA_MODEL = "kit_players"
        const val AUCTIONS_SET_NAME = "auctions"
        const val EXPIRED_AUCTIONS_SET_NAME = "expired_auctions"
        const val VAULT_MAP_NAME = "vaults"
        const val PROFILES_MAP_NAME = "profiles"
        const val PLAYER_SHOP_MAP_NAME = "player_shops"
        const val PROXY_DATA_BUCKET_NAME = "proxy_data"
        const val COMBAT_MAP_NAME = "combat"
    }

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ReqRes(val channel: String)

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Mapping(val id: String)

    data class Credential(val username: String, val password: String, val address: String, val port: Int)

    internal class Codec : BaseCodec() {
        companion object {
            const val MAPPING_FIELD = "mapping"
            const val DATA_FIELD = "data"
        }

        private val mappingsByClazz: MutableMap<KClass<*>, String> = mutableMapOf()
        private val mappingsById: MutableMap<String, KClass<*>> = mutableMapOf()

        /**
         * Initialize the mappings for the codec
         * Mappings are ID references to POJO's that are serialized and deserialized
         * ID's should not change once a mapping is created and actively used in production.
         * If a mapping is changed, the Redis map should be cleared to prevent deserialization errors.
         *
         * The overarching goal of the mapping system is to allow POJO classes
         * to be relocated or renamed without breaking the serialization/deserialization process.
         *
         * If a mapping does not exist for an object, the class name will be used as the mapping. (This is not recommended but required for classes such as string, int, etc.)
         */
        init {
            for (clazz in findAnnotatedClasses(Mapping::class.java)) {
                val mapping = clazz.getAnnotation(Mapping::class.java)
                mappingsById[mapping.id] = clazz.kotlin
                mappingsByClazz[clazz.kotlin] = mapping.id
            }
        }

        private val encoder = Encoder { obj ->
            try {
                val mapping = mappingsByClazz[obj::class]
                    ?: obj::class.java.name // If the class mapping is not registered, use the simple name

                val json = JsonObject().apply {
                    addProperty(MAPPING_FIELD, mapping)
                    addProperty(DATA_FIELD, Json.INSTANCE.toJson(obj))
                }

                Unpooled.wrappedBuffer(json.toString().toByteArray(StandardCharsets.UTF_8))
            } catch (e: Exception) {
                throw IOException("Error encoding object to JSON", e)
            }
        }

        private val decoder = Decoder { buf: ByteBuf, _: State? ->
            try {
                val jsonString = buf.toString(StandardCharsets.UTF_8)
                val json = JsonParser.parseString(jsonString).asJsonObject
                val mapping = json.get(MAPPING_FIELD).asString
                val data = json.get(DATA_FIELD).asString

                var clazz: KClass<*>? = mappingsById[mapping]
                if (clazz == null) {
                    clazz = mappingsById.computeIfAbsent(mapping) {
                        Class.forName(mapping).kotlin
                    }
                }

                Json.INSTANCE.fromJson(data, clazz.java)
            } catch (e: Exception) {
                throw IOException("Error decoding JSON to object", e)
            }
        }

        override fun getMapValueDecoder() = decoder
        override fun getMapValueEncoder() = encoder
        override fun getMapKeyDecoder() = decoder
        override fun getMapKeyEncoder() = encoder
        override fun getValueDecoder() = decoder
        override fun getValueEncoder() = encoder
    }

    val client: RedissonClient
    private val codec: Codec

    init {
        val config = Config()
        val instance = Codec()
        config.useSingleServer()
            .apply {
                this.address = "redis://${credential.address}:${credential.port}"
                this.password = credential.password
                this.username = credential.username
            }

        config.codec = instance
        this.codec = instance
        this.client = Redisson.create(config)
    }

    /**
     *
     */
    fun <K, V> transactional(
        mapName: String,
        operation: (RMap<K, V>) -> Unit,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (ThreadContext.forCurrentThread() != ThreadContext.ASYNC) {
            throw Exception("You must be in an async context to create transactions!")
        }

        val transaction = client.createTransaction(TransactionOptions.defaults())
        val map = transaction.getMap<K, V>(mapName)

        try {
            operation(map) // Execute the custom operation (put, remove, etc.)
            transaction.commit()
            onSuccess()
        } catch (e: Exception) {
            transaction.rollback()
            println("Transaction failed: ${e.message}")
            onFailure(e)
        }
    }


    /**
     * Delete a server by its island object.
     *
     * @param island The island object to delete.
     */
    fun deleteIsland(island: Island): RFuture<Island> {
        // Remove the island for all the members
        return islands().removeAsync(island.id)
    }

    /**
     * Query a server by an id asynchronously.
     *
     * @param id The server id to query.
     */
    fun getServer(id: String): RFuture<ServerDataModel?> {
        return servers().getAsync(id)
    }

    /**
     * Query a server entity that is prepared to be used that
     * is in the READY or ALLOCATED state.
     *
     * @param type The server type to query.
     */
    fun getReadyServer(type: ServerType): ServerDataModel {
        return servers().values.filter { it.type == type && (it.state == ServerState.READY || it.state == ServerState.ALLOCATED) }
            .minByOrNull { it.getUsedMemory() }
            ?: throw IllegalStateException("No servers available for type $type")
    }

    /**
     * Subscribe to a message payload for its respective channel. The inline type
     * must be annotated with the @ReqRes annotation to decipher which
     * channel to listen from.
     *
     * @param consumer The payload to handle.
     * @see ReqRes The annotation that specifies the channel id.
     */
    inline fun <reified T : Any> listen(noinline consumer: (T) -> Unit) {
        val meta = T::class.annotations.find { it is ReqRes } as? ReqRes
            ?: throw IllegalArgumentException("Class ${T::class} must have @ReqRes annotation")

        client.getTopic(meta.channel).addListenerAsync(T::class.java) { _, message ->
            consumer(message)
        }
    }

    /**
     * Publish a message object to its respective channel. The message
     * object's class must be annotated with a ReqRes annotation to
     * specify which channel to publish into.
     *
     * @param message The message object to publish.
     * @see ReqRes The annotation that specifies the channel id.
     */
    inline fun <reified T : Any> publish(message: T) {
        val meta = T::class.annotations.find { it is ReqRes } as? ReqRes
            ?: throw IllegalArgumentException("Class ${T::class} must have @ReqRes annotation")

        client.getTopic(meta.channel).publishAsync(message)
    }

    /**
     * Attempt to query a server that is already in the ALLOCATED stage or
     * query a READY state server if the specified memory usage threshold
     * has been reached or one is not available.
     *
     * @see ServerDataModel.MAX_MEMORY_THRESHOLD_PERCENTAGE
     */
    fun getServer(type: ServerType): ServerDataModel? {
        var allocated = servers().values.filter { it.type == type && it.state == ServerState.ALLOCATED }
            .minByOrNull { it.getUsedMemory() }

        // If there's no servers that are allocated, find a regular ready server
        if (allocated == null) {
            return servers().values.firstOrNull { it.type == type && it.state == ServerState.READY }
        }

        val usedMemory = allocated.getUsedMemory()
        val maxMemory = allocated.maxMemory
        val memoryUsagePercentage = (usedMemory.toDouble() / maxMemory) * 100

        if (memoryUsagePercentage >= ServerDataModel.MAX_MEMORY_THRESHOLD_PERCENTAGE) {
            allocated = servers().values.filter { it.type == type && it.state == ServerState.READY }
                .minByOrNull { it.getUsedMemory() }
        }

        return allocated
    }

    fun proxy(): RBucket<ProxyDataModel> {
        return client.getBucket(PROXY_DATA_BUCKET_NAME)
    }

    fun semaphores(id: String): RPermitExpirableSemaphore {
        return client.getPermitExpirableSemaphore(id).apply {
            this.trySetPermits(1)
        }
    }

    fun playerShops(): RMap<UUID, PlayerShopDataModel> {
        return client.getMap(PLAYER_SHOP_MAP_NAME)
    }

    fun profiles(): RMapCache<UUID, Pair<String, String>> {
        return client.getMapCache(PROFILES_MAP_NAME)
    }

    fun vaults(): RMap<UUID, VaultDataModel> {
        return client.getMap(VAULT_MAP_NAME)
    }

    fun economy(): RMap<UUID, PlayerEconomyModel> {
        return client.getMap(ECONOMY_MAP_NAME)
    }

    fun auctions(): RMapCache<UUID, AuctionHouseItem> {
        return client.getMapCache(AUCTIONS_SET_NAME)
    }

    fun expiredAuctions(): RListMultimap<UUID, AuctionHouseItem> {
        return client.getListMultimap(EXPIRED_AUCTIONS_SET_NAME)
    }

    fun kits(): RMap<UUID, KitPlayerDataModel> {
        return client.getMap(KIT_PLAYER_DATA_MODEL)
    }

    fun invites(): RListMultimapCache<UUID, UUID> {
        return client.getListMultimapCache(INVITES_FOR_MAP_NAME)
    }

    fun players(): RMap<UUID, PlayerDataModel> {
        return client.getMap(PLAYER_MAP_NAME)
    }

    fun servers(): RMap<String, ServerDataModel> {
        return client.getMap(SERVER_MAP_NAME)
    }

    fun targets(): RMapCache<UUID, UUID> {
        return client.getMapCache(MESSAGE_TARGET_MAP_NAME)
    }

    fun islands(): RMap<UUID, Island> {
        return client.getMap(ISLAND_MAP_NAME)
    }
}