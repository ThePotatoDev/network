package gg.tater.core.redis

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.tater.core.Json
import gg.tater.core.Mappings
import gg.tater.core.annotation.InvocationContext
import gg.tater.core.annotation.InvocationContextType
import gg.tater.core.annotation.Message
import gg.tater.core.proxy.ProxyDataModel
import gg.tater.core.server.model.ServerDataModel
import gg.tater.core.server.model.ServerState
import gg.tater.core.server.model.ServerType
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import me.lucko.helper.Services
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
import java.util.concurrent.CompletionStage
import kotlin.reflect.KClass

class Redis(credential: Credential) {
    private companion object {
        const val SERVER_MAP_NAME = "servers"
        const val PROXY_DATA_BUCKET_NAME = "proxy_data"
    }

    data class Credential(val username: String, val password: String, val address: String, val port: Int)

    internal class Codec : BaseCodec() {
        companion object {
            const val MAPPING_FIELD = "mapping"
            const val DATA_FIELD = "data"
        }

        private val encoder = Encoder { obj ->
            try {
                val mapping = Mappings.getMappingByClazz(obj::class)
                    ?: obj::class.java.name // If the class mapping is not registered, use the simple name

                val json = JsonObject().apply {
                    addProperty(MAPPING_FIELD, mapping)
                    addProperty(DATA_FIELD, Json.get().toJson(obj))
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

                var clazz: KClass<*>? = Mappings.getMappingById(mapping)
                if (clazz == null) {
                    clazz = Mappings.computeById(mapping)
                }

                Json.get().fromJson(data, clazz.java)
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
    @InvocationContext(InvocationContextType.ASYNC)
    fun getReadyServer(type: ServerType): ServerDataModel {
        return servers().values.filter { it.type == type && (it.state == ServerState.READY || it.state == ServerState.ALLOCATED) }
            .minByOrNull { it.getUsedMemory() }
            ?: throw IllegalStateException("No servers available for type $type")
    }

    /**
     * Retrieves a server entity asynchronously that is either READY or ALLOCATED.
     *
     * @param type The type of server to query.
     * @return The server data model in READY or ALLOCATED state with the least used memory.
     * @throws IllegalStateException if no servers are available for the specified type.
     */
    fun getReadyServerAsync(type: ServerType): CompletionStage<ServerDataModel> {
        return servers().readAllValuesAsync()
            .thenApplyAsync { it.filter { server -> server.type == type && (server.state == ServerState.READY || server.state == ServerState.ALLOCATED) } }
            .thenApplyAsync {
                it.minByOrNull { server -> server.getUsedMemory() }
                    ?: throw IllegalStateException("No servers available for type $type")
            }
    }

    /**
     * Subscribes to a message payload for its respective channel.
     * The inline type must be annotated with the @ReqRes annotation to determine the channel to listen from.
     *
     * @param consumer The function to handle the received payload.
     * @param T The type of the message payload.
     * @throws IllegalArgumentException if the class is not annotated with @ReqRes.
     * @see Message The annotation that specifies the channel ID.
     */
    inline fun <reified T : Any> listen(noinline consumer: (T) -> Unit) {
        val meta = T::class.annotations.find { it is Message } as? Message
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
     * @see Message The annotation that specifies the channel ID.
     */
    inline fun <reified T : Any> publish(message: T) {
        val meta = T::class.annotations.find { it is Message } as? Message
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

    /**
     * Deletes a server entry from the server map.
     *
     * @return The map containing server data models.
     */
    fun deleteServer(id: String): RFuture<ServerDataModel> {
        return client.getMap<String, ServerDataModel>(SERVER_MAP_NAME)
            .removeAsync(id)
    }
}

/**
 * Performs a transactional operation on a Redis map.
 * This will wait for the operation to complete before continuing.
 * Must be invoked in an async context.
 *
 * @param operation The operation to perform within the transaction.
 * @param onSuccess Callback to execute if the transaction is committed successfully.
 * @param onFailure Callback to execute if the transaction fails and is rolled back.
 * @throws Exception if not in an async context.
 */
@InvocationContext(type = InvocationContextType.ASYNC)
fun <K, V> RMap<K, V>.transactional(
    operation: (RMap<K, V>) -> Unit,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    if (ThreadContext.forCurrentThread() != ThreadContext.ASYNC) {
        throw Exception("You must be in an async context to create transactions!")
    }

    val transaction = Services.load(Redis::class.java).client.createTransaction(TransactionOptions.defaults())
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
