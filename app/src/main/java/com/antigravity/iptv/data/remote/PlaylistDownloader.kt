package com.antigravity.iptv.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

class PlaylistDownloader(
    private val userAgent: String = "VLC/3.0.18 LibVLC/3.0.18"
) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun <T> downloadStream(
        url: String,
        block: suspend (InputStream) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "*/*")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("HTTP error ${response.code}: ${response.message}")
                )
            }

            val body = response.body
                ?: return@withContext Result.failure(Exception("Response body is empty"))

            body.byteStream().use { inputStream ->
                val result = block(inputStream)
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(url: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .head()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                // If HEAD fails with 405 Method Not Allowed, try GET
                val getRequest = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()
                val getResponse = client.newCall(getRequest).execute()
                if (getResponse.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP error ${getResponse.code}: ${getResponse.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
