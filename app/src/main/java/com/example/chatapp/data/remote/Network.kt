package com.example.chatapp.data.remote

import com.example.chatapp.data.SessionManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Network {
    @Volatile private var api: ApiService? = null

    fun api(): ApiService {
        return api ?: synchronized(this) {
            api ?: build().also { api = it }
        }
    }

    /** Call before first use if the server address changed. */
    fun reset() {
        synchronized(this) { api = null }
    }

    private fun build(): ApiService {
        val auth = Interceptor { chain ->
            val token = try { SessionManager.get().token } catch (_: Exception) { "" }
            val req = if (token.isNotBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else chain.request()
            chain.proceed(req)
        }
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(ServerConfig.base())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(ApiService::class.java)
    }
}
