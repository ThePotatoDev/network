package gg.tater.core.network

import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class Agones(private val client: OkHttpClient) {

    private companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()

        private val BASE_CALLBACK = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { result ->
                    if (!result.isSuccessful) {
                        println("Failed to handle ${response.body}")
                        return
                    }
                }
            }
        }

        private val EMPTY_OBJECT = "{}".toRequestBody(JSON_MEDIA_TYPE)
    }

    fun allocate() {
        val request = Request.Builder()
            .url("http://localhost:9358/allocate")
            .post(EMPTY_OBJECT)
            .build()

        client.newCall(request)
            .execute()
            .use {}
    }

    fun ready() {
        val request = Request.Builder()
            .url("http://localhost:9358/ready")
            .post(EMPTY_OBJECT)
            .build()

        client.newCall(request).enqueue(BASE_CALLBACK)
    }

    fun health() {
        val request = Request.Builder()
            .url("http://localhost:9358/health")
            .post(EMPTY_OBJECT)
            .build()

        client.newCall(request)
            .execute()
            .use {}
    }

    fun shutdown() {
        val request = Request.Builder()
            .url("http://localhost:9358/shutdown")
            .post(EMPTY_OBJECT)
            .build()

        client.newCall(request)
            .execute()
            .use {}
    }

    fun getGameServerId(): String? {
        val request = Request.Builder()
            .url("http://localhost:9358/gameserver")
            .get()
            .build()

        client.newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }

                val json = JsonParser.parseString(response.body?.string()).asJsonObject
                return json.get("object_meta").asJsonObject.get("name").asString
            }
    }

}