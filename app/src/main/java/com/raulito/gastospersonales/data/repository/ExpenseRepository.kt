package com.raulito.gastospersonales.data.repository

import com.raulito.gastospersonales.data.local.AppDatabase
import com.raulito.gastospersonales.data.local.entity.CategoriaEntity
import com.raulito.gastospersonales.data.local.entity.GastoConCategoria
import com.raulito.gastospersonales.data.local.entity.GastoEntity
import com.raulito.gastospersonales.data.local.entity.GastoPorCategoria
import com.raulito.gastospersonales.data.local.entity.PresupuestoConCategoria
import com.raulito.gastospersonales.data.local.entity.PresupuestoEntity
import com.raulito.gastospersonales.data.remote.NetworkModule
import com.raulito.gastospersonales.data.remote.dto.RemoteGastoDto
import com.raulito.gastospersonales.data.remote.dto.RemotePresupuestoDto
import com.raulito.gastospersonales.notifications.BudgetNotifier
import com.raulito.gastospersonales.security.SecureTokenStorage
import com.raulito.gastospersonales.security.TextCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

sealed class SyncState {
    data object Idle : SyncState()
    data object Loading : SyncState()
    data class Success(val syncedCount: Int, val isDemoCloud: Boolean) : SyncState()
    data class Error(val message: String) : SyncState()
}

data class BudgetStatus(
    val presupuesto: PresupuestoEntity,
    val categoria: CategoriaEntity,
    val spent: Double,
    val progressRatio: Float
)

class ExpenseRepository(
    private val database: AppDatabase,
    private val crypto: TextCipher,
    private val tokenStorage: SecureTokenStorage,
    private val networkModule: NetworkModule,
    private val notifier: BudgetNotifier
) {
    private val gastoDao = database.gastoDao()
    private val categoriaDao = database.categoriaDao()
    private val presupuestoDao = database.presupuestoDao()

    suspend fun ensureSeeded() {
        withContext(Dispatchers.IO) {
            AppDatabase.seedDefaultsIfEmpty(database)
        }
    }

    fun observeCategorias(): Flow<List<CategoriaEntity>> = categoriaDao.observeAll()

    fun observeGastos(): Flow<List<GastoConCategoria>> =
        gastoDao.observeAllWithCategoria().map { list ->
            list.map { item ->
                val plainNote = crypto.decrypt(item.gasto.notaCifrada)
                item.copy(gasto = item.gasto.copy(notaCifrada = plainNote))
            }
        }

    fun observeWeeklyTotalsByCategory(): Flow<List<GastoPorCategoria>> =
        gastoDao.observeTotalsByCategorySince(startOfCurrentWeekMillis())

    fun observePresupuestos(): Flow<List<PresupuestoConCategoria>> =
        presupuestoDao.observeAllWithCategoria()

    suspend fun createOrUpdateGasto(
        id: Long = 0L,
        titulo: String,
        monto: Double,
        categoriaId: Long,
        notaPlana: String,
        rutaTicket: String?,
        fechaEpochMillis: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val userId = tokenStorage.getUserId() ?: "local_user"
        val existing = if (id > 0L) gastoDao.getById(id) else null
        val encryptedNote = if (notaPlana.isBlank()) "" else crypto.encrypt(notaPlana.trim())
        val entity = GastoEntity(
            id = id,
            titulo = titulo.trim(),
            monto = monto,
            fechaEpochMillis = fechaEpochMillis,
            notaCifrada = encryptedNote,
            rutaTicket = rutaTicket ?: existing?.rutaTicket,
            categoriaId = categoriaId,
            usuarioId = userId,
            sincronizado = false,
            remoteId = existing?.remoteId,
            updatedAt = System.currentTimeMillis()
        )
        val savedId = if (id == 0L) {
            gastoDao.insert(entity)
        } else {
            gastoDao.update(entity)
            id
        }
        checkBudgetForCategory(categoriaId)
        savedId
    }

    suspend fun deleteGasto(gasto: GastoEntity) = withContext(Dispatchers.IO) {
        gastoDao.deleteById(gasto.id)
    }

    suspend fun upsertPresupuesto(
        categoriaId: Long,
        montoLimite: Double,
        periodo: String,
        alertaPorcentaje: Int
    ) = withContext(Dispatchers.IO) {
        val existing = presupuestoDao.getByCategoriaId(categoriaId)
        val entity = PresupuestoEntity(
            id = existing?.id ?: 0L,
            categoriaId = categoriaId,
            montoLimite = montoLimite,
            periodo = periodo,
            alertaPorcentaje = alertaPorcentaje,
            activo = true
        )
        presupuestoDao.insert(entity)
        checkBudgetForCategory(categoriaId)
    }

    suspend fun deletePresupuesto(presupuesto: PresupuestoEntity) = withContext(Dispatchers.IO) {
        presupuestoDao.delete(presupuesto)
    }

    suspend fun getBudgetStatuses(): List<BudgetStatus> = withContext(Dispatchers.IO) {
        presupuestoDao.getActiveWithCategoriaOnce().map { rel ->
            val since = periodStartMillis(rel.presupuesto.periodo)
            val spent = gastoDao.getSumForCategorySince(rel.categoria.id, since)
            val ratio = if (rel.presupuesto.montoLimite > 0) {
                (spent / rel.presupuesto.montoLimite).toFloat()
            } else 0f
            BudgetStatus(
                presupuesto = rel.presupuesto,
                categoria = rel.categoria,
                spent = spent,
                progressRatio = ratio
            )
        }
    }

    suspend fun evaluateBudgetsAndNotify(forceSampleIfNoneExceeded: Boolean = false): Int =
        withContext(Dispatchers.IO) {
            val statuses = getBudgetStatuses()
            var sent = 0
            statuses.forEach { status ->
                val threshold = status.presupuesto.alertaPorcentaje / 100f
                if (status.progressRatio >= threshold) {
                    val ok = notifier.notifyBudgetExceeded(
                        categoryName = status.categoria.nombre,
                        period = status.presupuesto.periodo,
                        spent = status.spent,
                        limit = status.presupuesto.montoLimite,
                        alertPercent = status.presupuesto.alertaPorcentaje,
                        notificationId = 1000 + status.categoria.id.toInt()
                    )
                    if (ok) sent++
                }
            }
            if (sent == 0 && forceSampleIfNoneExceeded && statuses.isNotEmpty()) {
                val sample = statuses.first()
                val simulatedSpent = sample.presupuesto.montoLimite * 0.85
                val ok = notifier.notifyBudgetExceeded(
                    categoryName = sample.categoria.nombre,
                    period = sample.presupuesto.periodo,
                    spent = simulatedSpent,
                    limit = sample.presupuesto.montoLimite,
                    alertPercent = sample.presupuesto.alertaPorcentaje,
                    notificationId = 9999
                )
                if (ok) sent++
            }
            sent
        }

    private suspend fun checkBudgetForCategory(categoriaId: Long) {
        val presupuesto = presupuestoDao.getByCategoriaId(categoriaId) ?: return
        if (!presupuesto.activo || presupuesto.montoLimite <= 0.0) return
        val categoria = categoriaDao.getById(categoriaId) ?: return
        val spent = gastoDao.getSumForCategorySince(categoriaId, periodStartMillis(presupuesto.periodo))
        val percent = (spent / presupuesto.montoLimite) * 100.0
        if (percent >= presupuesto.alertaPorcentaje) {
            notifier.notifyBudgetExceeded(
                categoryName = categoria.nombre,
                period = presupuesto.periodo,
                spent = spent,
                limit = presupuesto.montoLimite,
                alertPercent = presupuesto.alertaPorcentaje,
                notificationId = 1000 + categoria.id.toInt()
            )
        }
    }

    suspend fun syncWithCloud(): SyncState = withContext(Dispatchers.IO) {
        val token = tokenStorage.getAccessToken()
            ?: return@withContext SyncState.Error("Sesión no válida. Inicia sesión nuevamente.")
        val userId = tokenStorage.getUserId() ?: "local_user"
        val bearer = "Bearer $token"

        return@withContext runCatching {
            val api = networkModule.restApi()
            val unsynced = gastoDao.getUnsynced()
            var syncedCount = 0

            if (unsynced.isNotEmpty()) {
                val payload = unsynced.map { gasto ->
                    RemoteGastoDto(
                        id = gasto.remoteId,
                        localId = gasto.id,
                        userId = userId,
                        titulo = gasto.titulo,
                        monto = gasto.monto,
                        fechaEpochMillis = gasto.fechaEpochMillis,
                        categoriaId = gasto.categoriaId,
                        updatedAt = gasto.updatedAt
                    )
                }
                val pushResp = api.upsertGastos(bearer, payload)
                if (!pushResp.isSuccessful) {
                    return@runCatching SyncState.Error("Error al subir gastos (HTTP ${pushResp.code()})")
                }
                val savedRemotes = pushResp.body().orEmpty()
                unsynced.forEachIndexed { idx, local ->
                    val remoteId = savedRemotes.getOrNull(idx)?.id ?: local.remoteId ?: "remote_${local.id}"
                    gastoDao.markSynced(local.id, remoteId)
                    syncedCount++
                }
            }

            val budgets = presupuestoDao.getActiveWithCategoriaOnce()
            if (budgets.isNotEmpty()) {
                val budgetPayload = budgets.map { rel ->
                    RemotePresupuestoDto(
                        userId = userId,
                        categoriaId = rel.presupuesto.categoriaId,
                        montoLimite = rel.presupuesto.montoLimite,
                        periodo = rel.presupuesto.periodo,
                        alertaPorcentaje = rel.presupuesto.alertaPorcentaje
                    )
                }
                api.upsertPresupuestos(bearer, budgetPayload)
            }

            val pullResp = api.fetchGastos(bearer, "eq.$userId")
            if (pullResp.isSuccessful) {
                val existingIds = gastoDao.getAllOnce().mapNotNull { it.remoteId }.toSet()
                pullResp.body().orEmpty().forEach { remote ->
                    if (!remote.id.isNullOrBlank() && remote.id !in existingIds) {
                        val existsLocally = gastoDao.getById(remote.localId) != null
                        if (!existsLocally) {
                            gastoDao.insert(
                                GastoEntity(
                                    titulo = remote.titulo,
                                    monto = remote.monto,
                                    fechaEpochMillis = remote.fechaEpochMillis,
                                    notaCifrada = "",
                                    categoriaId = remote.categoriaId,
                                    usuarioId = remote.userId,
                                    sincronizado = true,
                                    remoteId = remote.id,
                                    updatedAt = remote.updatedAt
                                )
                            )
                            syncedCount++
                        }
                    }
                }
            }

            SyncState.Success(
                syncedCount = syncedCount,
                isDemoCloud = !networkModule.isLiveSupabaseConfigured()
            )
        }.getOrElse { ex ->
            SyncState.Error("Error de sincronización: ${ex.localizedMessage ?: "Sin conexión"}")
        }
    }

    private fun periodStartMillis(periodo: String): Long {
        return if (periodo == "MENSUAL") startOfCurrentMonthMillis() else startOfCurrentWeekMillis()
    }

    companion object {
        fun startOfCurrentWeekMillis(): Long {
            val today = LocalDate.now()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun startOfCurrentMonthMillis(): Long {
            val firstDay = LocalDate.now().withDayOfMonth(1)
            return firstDay.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
}
