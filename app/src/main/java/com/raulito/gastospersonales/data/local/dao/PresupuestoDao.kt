package com.raulito.gastospersonales.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.raulito.gastospersonales.data.local.entity.PresupuestoConCategoria
import com.raulito.gastospersonales.data.local.entity.PresupuestoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresupuestoDao {
    @Transaction
    @Query("SELECT * FROM presupuestos ORDER BY id ASC")
    fun observeAllWithCategoria(): Flow<List<PresupuestoConCategoria>>

    @Transaction
    @Query("SELECT * FROM presupuestos WHERE activo = 1")
    suspend fun getActiveWithCategoriaOnce(): List<PresupuestoConCategoria>

    @Query("SELECT * FROM presupuestos WHERE categoriaId = :categoriaId LIMIT 1")
    suspend fun getByCategoriaId(categoriaId: Long): PresupuestoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PresupuestoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PresupuestoEntity>)

    @Update
    suspend fun update(item: PresupuestoEntity)

    @Delete
    suspend fun delete(item: PresupuestoEntity)

    @Query("SELECT COUNT(*) FROM presupuestos")
    suspend fun count(): Int
}
