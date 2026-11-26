package com.raulito.gastospersonales.data.remote

import com.google.gson.Gson
import com.raulito.gastospersonales.BuildConfig
import com.raulito.gastospersonales.data.remote.dto.AuthRequest
import com.raulito.gastospersonales.data.remote.dto.AuthResponse
import com.raulito.gastospersonales.data.remote.dto.RemoteGastoDto
import com.raulito.gastospersonales.data.remote.dto.SupabaseUserDto
import com.raulito.gastospersonales.security.SecureTokenStorage
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Configures Retrofit + OkHttp for Supabase Auth & PostgREST synchronization.
 * Redacts sensitive headers (`Authorization`, `apikey`) in HTTP logs.
 * When SUPABASE_URL is not configured, routes requests through a realistic local REST
 * simulation interceptor so all HTTP states (loading, 200, 401, sync) work out-of-the-box.
 */
class NetworkModule(private val tokenStorage: SecureTokenStorage) {

    private val gson = Gson()
    private val demoCloudExpenses = CopyOnWriteArrayList<RemoteGastoDto>()

    fun isLiveSupabaseConfigured(): Boolean {
        return resolveBaseUrl() != DEMO_BASE_URL && resolveAnonKey().isNotBlank()
    }

    fun resolveBaseUrl(): String {
        val custom = tokenStorage.getCustomSupabaseUrl().trim()
        val configured = custom.ifBlank { BuildConfig.SUPABASE_URL.trim() }
        if (configured.isBlank() || !configured.startsWith("http")) {
            return DEMO_BASE_URL
        }
        return if (configured.endsWith("/")) configured else "$configured/"
    }

    fun resolveAnonKey(): String {
        val custom = tokenStorage.getCustomSupabaseKey().trim()
        return custom.ifBlank { BuildConfig.SUPABASE_ANON_KEY.trim() }
    }

    private fun buildClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("apikey")
        }

        val headerInterceptor = Interceptor { chain ->
            val apiKey = resolveAnonKey().ifBlank { "demo-anon-key" }
            val req = chain.request().newBuilder()
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }

        val demoFallbackInterceptor = Interceptor { chain ->
            val request = chain.request()
            if (request.url.host != "demo.supabase.local") {
                return@Interceptor chain.proceed(request)
            }
            simulateSupabaseHttp(request)
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(headerInterceptor)
            .addInterceptor(demoFallbackInterceptor)
            .addInterceptor(logging)
            .build()
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(resolveBaseUrl())
            .client(buildClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun authApi(): SupabaseAuthApi = buildRetrofit().create(SupabaseAuthApi::class.java)

    fun restApi(): SupabaseRestApi = buildRetrofit().create(SupabaseRestApi::class.java)

    private fun simulateSupabaseHttp(request: okhttp3.Request): Response {
        Thread.sleep(450)
        val path = request.url.encodedPath
        val jsonMedia = "application/json".toMediaType()

        fun jsonResponse(code: Int, bodyObj: Any): Response {
            val json = gson.toJson(bodyObj)
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code in 200..299) "OK" else "Error")
                .body(json.toResponseBody(jsonMedia))
                .build()
        }

        return when {
            path.endsWith("/auth/v1/signup") || path.endsWith("/auth/v1/token") -> {
                val rawBody = readRequestBody(request)
                val authReq = gson.fromJson(rawBody, AuthRequest::class.java)
                val userId = UUID.nameUUIDFromBytes(authReq.email.lowercase().toByteArray()).toString()
                val token = "eyJhbGciOiJIUzI1NiJ9.${UUID.randomUUID()}.demo"
                jsonResponse(
                    200,
                    AuthResponse(
                        accessToken = token,
                        refreshToken = "refresh_${UUID.randomUUID()}",
                        expiresIn = 3600L,
                        tokenType = "bearer",
                        user = SupabaseUserDto(id = userId, email = authReq.email.trim())
                    )
                )
            }
            path.endsWith("/auth/v1/logout") -> {
                jsonResponse(204, emptyMap<String, String>())
            }
            path.endsWith("/rest/v1/gastos") && request.method == "POST" -> {
                val rawBody = readRequestBody(request)
                val incoming = gson.fromJson(rawBody, Array<RemoteGastoDto>::class.java).toList()
                val saved = incoming.map { dto ->
                    val withId = if (dto.id.isNullOrBlank()) dto.copy(id = UUID.randomUUID().toString()) else dto
                    demoCloudExpenses.removeAll { it.localId == withId.localId && it.userId == withId.userId }
                    demoCloudExpenses.add(withId)
                    withId
                }
                jsonResponse(201, saved)
            }
            path.endsWith("/rest/v1/gastos") && request.method == "GET" -> {
                jsonResponse(200, demoCloudExpenses.toList())
            }
            path.endsWith("/rest/v1/presupuestos") && request.method == "POST" -> {
                val rawBody = readRequestBody(request)
                val list = gson.fromJson(rawBody, Array<Any>::class.java).toList()
                jsonResponse(201, list)
            }
            else -> jsonResponse(404, mapOf("message" to "Endpoint no encontrado"))
        }
    }

    private fun readRequestBody(request: okhttp3.Request): String {
        val body = request.body ?: return ""
        val buffer = Buffer()
        body.writeTo(buffer)
        return buffer.readUtf8()
    }

    companion object {
        const val DEMO_BASE_URL = "https://demo.supabase.local/"
    }
}
