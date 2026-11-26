package com.raulito.gastospersonales.data.repository

import com.google.gson.Gson
import com.raulito.gastospersonales.data.remote.NetworkModule
import com.raulito.gastospersonales.data.remote.dto.AuthErrorDto
import com.raulito.gastospersonales.data.remote.dto.AuthRequest
import com.raulito.gastospersonales.security.InputValidator
import com.raulito.gastospersonales.security.SecureTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class AuthState {
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    data class Authenticated(
        val userId: String,
        val email: String,
        val isDemoCloud: Boolean
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository(
    private val networkModule: NetworkModule,
    private val tokenStorage: SecureTokenStorage
) {
    private val gson = Gson()
    private val _authState = MutableStateFlow(initialState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private fun initialState(): AuthState {
        return if (tokenStorage.isSessionValid() && !tokenStorage.isBiometricEnabled()) {
            AuthState.Authenticated(
                userId = tokenStorage.getUserId().orEmpty(),
                email = tokenStorage.getUserEmail().orEmpty(),
                isDemoCloud = !networkModule.isLiveSupabaseConfigured()
            )
        } else {
            AuthState.Unauthenticated
        }
    }

    fun hasSavedBiometricSession(): Boolean {
        return tokenStorage.isBiometricEnabled() && tokenStorage.isSessionValid()
    }

    fun unlockWithBiometricSession() {
        if (tokenStorage.isSessionValid()) {
            _authState.value = AuthState.Authenticated(
                userId = tokenStorage.getUserId().orEmpty(),
                email = tokenStorage.getUserEmail().orEmpty(),
                isDemoCloud = !networkModule.isLiveSupabaseConfigured()
            )
        } else {
            _authState.value = AuthState.Error("La sesión expiró. Inicia sesión con tu contraseña.")
        }
    }

    suspend fun signIn(email: String, password: String) {
        authenticate(email = email, password = password, isSignUp = false)
    }

    suspend fun signUp(email: String, password: String) {
        authenticate(email = email, password = password, isSignUp = true)
    }

    private suspend fun authenticate(email: String, password: String, isSignUp: Boolean) {
        val emailCheck = InputValidator.validateEmail(email)
        if (!emailCheck.isValid) {
            _authState.value = AuthState.Error(emailCheck.errorOrNull!!)
            return
        }
        val passCheck = InputValidator.validatePassword(password)
        if (!passCheck.isValid) {
            _authState.value = AuthState.Error(passCheck.errorOrNull!!)
            return
        }

        _authState.value = AuthState.Loading
        withContext(Dispatchers.IO) {
            runCatching {
                val api = networkModule.authApi()
                val request = AuthRequest(email = email.trim(), password = password)
                val response = if (isSignUp) api.signUp(request) else api.signInWithPassword(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.accessToken
                    val userId = body?.user?.id ?: "user_${email.trim().hashCode()}"
                    if (!token.isNullOrBlank()) {
                        val expiresAt = (System.currentTimeMillis() / 1000L) + (body.expiresIn ?: 3600L)
                        tokenStorage.saveSession(
                            accessToken = token,
                            refreshToken = body.refreshToken.orEmpty(),
                            userId = userId,
                            email = body.user?.email ?: email.trim(),
                            expiresAtEpochSec = expiresAt
                        )
                        _authState.value = AuthState.Authenticated(
                            userId = userId,
                            email = body.user?.email ?: email.trim(),
                            isDemoCloud = !networkModule.isLiveSupabaseConfigured()
                        )
                    } else if (isSignUp) {
                        _authState.value = AuthState.Error(
                            "Cuenta creada. Revisa tu correo para confirmar antes de iniciar sesión."
                        )
                    } else {
                        _authState.value = AuthState.Error("Respuesta de autenticación sin token válido.")
                    }
                } else {
                    val errRaw = response.errorBody()?.string()
                    val parsed = runCatching { gson.fromJson(errRaw, AuthErrorDto::class.java) }.getOrNull()
                    _authState.value = AuthState.Error(
                        parsed?.bestMessage() ?: "Error HTTP ${response.code()} al autenticar."
                    )
                }
            }.onFailure { throwable ->
                _authState.value = AuthState.Error(
                    "No se pudo conectar al servidor: ${throwable.localizedMessage ?: "Error de red"}"
                )
            }
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            val token = tokenStorage.getAccessToken()
            if (!token.isNullOrBlank()) {
                runCatching { networkModule.authApi().signOut("Bearer $token") }
            }
            tokenStorage.clearSession()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun updateCloudConfig(url: String, anonKey: String) {
        tokenStorage.saveCloudConfig(url, anonKey)
    }

    fun getCloudConfig(): Pair<String, String> =
        tokenStorage.getCustomSupabaseUrl() to tokenStorage.getCustomSupabaseKey()

    fun isLiveSupabaseConfigured(): Boolean = networkModule.isLiveSupabaseConfigured()

    fun isBiometricEnabled(): Boolean = tokenStorage.isBiometricEnabled()

    fun setBiometricEnabled(enabled: Boolean) {
        tokenStorage.setBiometricEnabled(enabled)
    }
}
