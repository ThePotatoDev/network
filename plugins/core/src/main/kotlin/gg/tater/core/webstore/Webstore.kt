package gg.tater.core.webstore

import com.google.gson.JsonParser
import me.lucko.helper.Services
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private const val PAYMENT_ENDPOINT = "https://plugin.tebex.io/payments"

fun getLatestPayments() {
    val request = Request.Builder()
        .url(PAYMENT_ENDPOINT)
        .get()
        .build()

    val client = Services.load(OkHttpClient::class.java)

    return Services.load(OkHttpClient::class.java)
        .newCall(request)
        .execute()
        .use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            JsonParser.parseString(response.body?.string())
                .asJsonObject
                .let {

                }
        }
}