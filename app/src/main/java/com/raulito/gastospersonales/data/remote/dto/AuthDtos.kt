package com.raulito.gastospersonales.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Long?,
    @SerializedName("token_type") val tokenType: String?,
    val user: SupabaseUserDto?
)

data class SupabaseUserDto(
    val id: String,
    val email: String?
)

data class AuthErrorDto(
    val error: String?,
    @SerializedName("error_description") val errorDescription: String?,
    val msg: String?,
    val message: String?
) {
    fun bestMessage(): String =
        errorDescription ?: msg ?: message ?: error ?: "Error de autenticación"
}
