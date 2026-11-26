package com.raulito.gastospersonales.data.remote

import com.raulito.gastospersonales.data.remote.dto.AuthRequest
import com.raulito.gastospersonales.data.remote.dto.AuthResponse
import com.raulito.gastospersonales.data.remote.dto.RemoteGastoDto
import com.raulito.gastospersonales.data.remote.dto.RemotePresupuestoDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {
    @POST("auth/v1/signup")
    suspend fun signUp(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/v1/logout")
    suspend fun signOut(@Header("Authorization") bearerToken: String): Response<Unit>
}

interface SupabaseRestApi {
    @GET("rest/v1/gastos?select=*")
    suspend fun fetchGastos(
        @Header("Authorization") bearerToken: String,
        @Query("user_id") userIdFilter: String
    ): Response<List<RemoteGastoDto>>

    @Headers("Prefer: return=representation,resolution=merge-duplicates")
    @POST("rest/v1/gastos")
    suspend fun upsertGastos(
        @Header("Authorization") bearerToken: String,
        @Body payload: List<RemoteGastoDto>
    ): Response<List<RemoteGastoDto>>

    @Headers("Prefer: return=representation,resolution=merge-duplicates")
    @POST("rest/v1/presupuestos")
    suspend fun upsertPresupuestos(
        @Header("Authorization") bearerToken: String,
        @Body payload: List<RemotePresupuestoDto>
    ): Response<List<RemotePresupuestoDto>>
}
