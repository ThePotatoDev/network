package gg.tater.shared.redis

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.tater.shared.Json
import gg.tater.shared.findAnnotatedClasses
import gg.tater.shared.network.ProxyDataModel
import gg.tater.shared.network.server.ServerDataModel
import gg.tater.shared.network.server.ServerState
import gg.tater.shared.network.server.ServerType
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
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class Redis(credential: Credential) {

    companion object {
        const val SERVER_MAP_NAME = "servers"
        const val PROXY_DATA_BUCKET_NAME = "proxy_data"
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
            for (clazz in findAnnotatedClasses(Mapping::class)) {
                val mapping = clazz.findAnnotation<Mapping>() ?: continue
                mappingsById[mapping.id] = clazz
                mappingsByClazz[clazz] = mapping.id
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
     * Asynchronously queries a server by its ID.
     *
     * @param id The unique identifier of the server to query.
     * @return A future containing the server data model, or null if not found.
     */
    fun getServer(id: String): RFuture<ServerDataModel?> {
        return servers().getAsync(id)
    }

    /**
     * Retrieves a server entity that is either READY or ALLOCATED.
     *
     * @param type The type of server to query.
     * @return The server data model in READY or ALLOCATED state with the least used memory.
     * @throws IllegalStateException if no servers are available for the specified type.
     */
    fun getReadyServer(type: ServerType): ServerDataModel {
        return servers().values.filter { it.type == type && (it.state == ServerState.READY || it.state == ServerState.ALLOCATED) }
            .minByOrNull { it.getUsedMemory() }
            ?: throw IllegalStateException("No servers available for type $type")
    }

    /**
     * Subscribes to a message payload for its respective channel.
     * The inline type must be annotated with the @ReqRes annotation to determine the channel to listen from.
     *
     * @param consumer The function to handle the received payload.
     * @param T The type of the message payload.
     * @throws IllegalArgumentException if the class is not annotated with @ReqRes.
     * @see ReqRes The annotation that specifies the channel ID.
     */
    inline fun <reified T : Any> listen(noinline consumer: (T) -> Unit) {
        val meta = T::class.annotations.find { it is ReqRes } as? ReqRes
            ?: throw IllegalArgumentException("Class ${T::class} must have @ReqRes annotation")

        client.getTopic(meta.channel).addListenerAsync(T::class.java) { _, message ->
            consumer(message)
        }
    }

    /**
     * Publishes a message object to its respective channel.
     * The message object's class must be annotated with a ReqRes annotation to specify the channel.
     *
     * @param message The message object to publish.
     * @param T The type of the message payload.
     * @throws IllegalArgumentException if the class is not annotated with @ReqRes.
     * @see ReqRes The annotation that specifies the channel ID.
     */
    inline fun <reified T : Any> publish(message: T) {
        val meta = T::class.annotations.find { it is ReqRes } as? ReqRes
            ?: throw IllegalArgumentException("Class ${T::class} must have @ReqRes annotation")

        client.getTopic(meta.channel).publishAsync(message)
    }

    /**
     * Attempts to query a server that is already in the ALLOCATED state,
     * or queries a READY state server if the specified memory usage threshold
     * has been reached or if an ALLOCATED server is not available.
     *
     * @param type The type of server to query.
     * @return The server data model, or null if none are available.
     * @see ServerDataModel.MAX_MEMORY_THRESHOLD_PERCENTAGE
     */
    fun getServer(type: ServerType): ServerDataModel? {
        var allocated = servers().values.filter { it.type == type && it.state == ServerState.ALLOCATED }
            .minByOrNull { it.getUsedMemory() }

        // If there's no allocated servers, find a ready server
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

    /**
     * Retrieves the proxy data bucket.
     *
     * @return The bucket containing proxy data.
     */
    fun proxy(): RBucket<ProxyDataModel> {
        return client.getBucket(PROXY_DATA_BUCKET_NAME)
    }

    /**
     * Retrieves a permit expirable semaphore by ID.
     *
     * @param id The unique identifier of the semaphore.
     * @return The permit expirable semaphore.
     */
    fun semaphores(id: String): RPermitExpirableSemaphore {
        return client.getPermitExpirableSemaphore(id).apply {
            this.trySetPermits(1)
        }
    }

    /**
     * Retrieves the map of server data models.
     *
     * @return The map containing server data models.
     */
    fun servers(): RMap<String, ServerDataModel> {
        return client.getMap(SERVER_MAP_NAME)
    }
}

/**
 * Performs a transactional operation on a Redis map.
 * This will wait for the operation to complete before continuing.
 * Must be invoked in an async context.
 *
 * @param client The Redisson client used to create the transaction.
 * @param operation The operation to perform within the transaction.
 * @param onSuccess Callback to execute if the transaction is committed successfully.
 * @param onFailure Callback to execute if the transaction fails and is rolled back.
 * @throws Exception if not in an async context.
 */
fun <K, V> RMap<K, V>.transactional(
    client: RedissonClient,
    operation: (RMap<K, V>) -> Unit,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    if (ThreadContext.forCurrentThread() != ThreadContext.ASYNC) {
        throw Exception("You must be in an async context to create transactions!")
    }

    val transaction = client.createTransaction(TransactionOptions.defaults())
    val transactionalMap = transaction.getMap<K, V>(this.name)

    try {
        operation(transactionalMap) // Execute the custom operation
        transaction.commit()
        onSuccess()
    } catch (e: Exception) {
        transaction.rollback()
        println("Transaction failed: ${e.message}")
        onFailure(e)
    }
}
