package com.raulito.gastospersonales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.raulito.gastospersonales.data.local.entity.CategoriaEntity
import com.raulito.gastospersonales.data.local.entity.GastoConCategoria
import com.raulito.gastospersonales.data.local.entity.GastoEntity
import com.raulito.gastospersonales.data.local.entity.GastoPorCategoria
import com.raulito.gastospersonales.data.local.entity.PresupuestoConCategoria
import com.raulito.gastospersonales.data.local.entity.PresupuestoEntity
import com.raulito.gastospersonales.data.repository.AuthRepository
import com.raulito.gastospersonales.data.repository.AuthState
import com.raulito.gastospersonales.data.repository.ExpenseRepository
import com.raulito.gastospersonales.data.repository.SyncState
import com.raulito.gastospersonales.security.InputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardMetrics(
    val totalSemanal: Double = 0.0,
    val totalPresupuestoSemanal: Double = 0.0,
    val pendientesSync: Int = 0,
    val totalesPorCategoria: List<GastoPorCategoria> = emptyList()
)

class MainViewModel(
    private val authRepository: AuthRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    val categorias: StateFlow<List<CategoriaEntity>> =
        expenseRepository.observeCategorias()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gastos: StateFlow<List<GastoConCategoria>> =
        expenseRepository.observeGastos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presupuestos: StateFlow<List<PresupuestoConCategoria>> =
        expenseRepository.observePresupuestos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyTotals: StateFlow<List<GastoPorCategoria>> =
        expenseRepository.observeWeeklyTotalsByCategory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardMetrics: StateFlow<DashboardMetrics> =
        combine(gastos, presupuestos, weeklyTotals) { gastosList, presupList, totalsList ->
            val weekStart = ExpenseRepository.startOfCurrentWeekMillis()
            val totalSemanal = gastosList
                .filter { it.gasto.fechaEpochMillis >= weekStart }
                .sumOf { it.gasto.monto }
            val totalPresup = presupList
                .filter { it.presupuesto.activo && it.presupuesto.periodo == "SEMANAL" }
                .sumOf { it.presupuesto.montoLimite }
            val pending = gastosList.count { !it.gasto.sincronizado }
            DashboardMetrics(
                totalSemanal = totalSemanal,
                totalPresupuestoSemanal = totalPresup,
                pendientesSync = pending,
                totalesPorCategoria = totalsList
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedCategoryFilter: StateFlow<Long?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            expenseRepository.ensureSeeded()
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signIn(email, password)
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signUp(email, password)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearAuthError() = authRepository.clearError()

    fun hasSavedBiometricSession(): Boolean = authRepository.hasSavedBiometricSession()

    fun unlockWithBiometricSession() = authRepository.unlockWithBiometricSession()

    fun isBiometricEnabled(): Boolean = authRepository.isBiometricEnabled()

    fun setBiometricEnabled(enabled: Boolean) {
        authRepository.setBiometricEnabled(enabled)
        _statusBanner.value = if (enabled) {
            "Bloqueo biométrico activado para el próximo inicio."
        } else {
            "Bloqueo biométrico desactivado."
        }
    }

    fun getCloudConfig(): Pair<String, String> = authRepository.getCloudConfig()

    fun isLiveSupabaseConfigured(): Boolean = authRepository.isLiveSupabaseConfigured()

    fun saveCloudConfig(url: String, key: String) {
        authRepository.updateCloudConfig(url, key)
        _statusBanner.value = if (url.isBlank()) {
            "Modo Demo REST local activado."
        } else {
            "Configuración de Supabase guardada."
        }
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveGasto(
        editingId: Long,
        titulo: String,
        rawMonto: String,
        categoriaId: Long,
        nota: String,
        rutaTicket: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanTitle = InputValidator.sanitizeText(titulo)
        val titleCheck = InputValidator.validateTitle(cleanTitle)
        if (!titleCheck.isValid) {
            onError(titleCheck.errorOrNull!!)
            return
        }
        val amountCheck = InputValidator.validateAmount(rawMonto)
        if (!amountCheck.isValid) {
            onError(amountCheck.errorOrNull!!)
            return
        }
        val noteCheck = InputValidator.validateNote(nota)
        if (!noteCheck.isValid) {
            onError(noteCheck.errorOrNull!!)
            return
        }
        val monto = rawMonto.trim().replace(',', '.').toDouble()
        viewModelScope.launch {
            expenseRepository.createOrUpdateGasto(
                id = editingId,
                titulo = cleanTitle,
                monto = monto,
                categoriaId = categoriaId,
                notaPlana = nota,
                rutaTicket = rutaTicket
            )
            _statusBanner.value = if (editingId == 0L) "Gasto registrado correctamente." else "Gasto actualizado."
            onSuccess()
        }
    }

    fun deleteGasto(gasto: GastoEntity) {
        viewModelScope.launch {
            expenseRepository.deleteGasto(gasto)
            _statusBanner.value = "Gasto eliminado."
        }
    }

    fun savePresupuesto(
        categoriaId: Long,
        rawLimite: String,
        periodo: String,
        alertaPorcentaje: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val check = InputValidator.validateBudgetLimit(rawLimite, alertaPorcentaje)
        if (!check.isValid) {
            onError(check.errorOrNull!!)
            return
        }
        val limite = rawLimite.trim().replace(',', '.').toDouble()
        viewModelScope.launch {
            expenseRepository.upsertPresupuesto(
                categoriaId = categoriaId,
                montoLimite = limite,
                periodo = periodo,
                alertaPorcentaje = alertaPorcentaje
            )
            _statusBanner.value = "Presupuesto guardado."
            onSuccess()
        }
    }

    fun deletePresupuesto(presupuesto: PresupuestoEntity) {
        viewModelScope.launch {
            expenseRepository.deletePresupuesto(presupuesto)
            _statusBanner.value = "Presupuesto eliminado."
        }
    }

    fun syncCloud() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            _syncState.value = expenseRepository.syncWithCloud()
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    fun triggerBudgetCheckNotification() {
        viewModelScope.launch {
            val sent = expenseRepository.evaluateBudgetsAndNotify(forceSampleIfNoneExceeded = true)
            _statusBanner.value = if (sent > 0) {
                "Notificación de presupuesto enviada ($sent)."
            } else {
                "Otorga el permiso de notificaciones para recibir alertas."
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val expenseRepository: ExpenseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(authRepository, expenseRepository) as T
        }
    }
}
