package com.raulito.gastospersonales.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.raulito.gastospersonales.data.local.entity.GastoConCategoria
import com.raulito.gastospersonales.data.local.entity.GastoEntity
import com.raulito.gastospersonales.data.local.entity.GastoPorCategoria
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {
    @Transaction
    @Query("SELECT * FROM gastos ORDER BY fechaEpochMillis DESC, id DESC")
    fun observeAllWithCategoria(): Flow<List<GastoConCategoria>>

    @Query(
        """
        SELECT c.id AS categoriaId, c.nombre AS nombreCategoria, c.colorHex AS colorHex,
               COALESCE(SUM(g.monto), 0.0) AS total
        FROM categorias c
        LEFT JOIN gastos g ON g.categoriaId = c.id AND g.fechaEpochMillis >= :sinceEpochMillis
        GROUP BY c.id
        ORDER BY total DESC
        """
    )
    fun observeTotalsByCategorySince(sinceEpochMillis: Long): Flow<List<GastoPorCategoria>>

    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM gastos WHERE categoriaId = :categoriaId AND fechaEpochMillis >= :sinceEpochMillis")
    suspend fun getSumForCategorySince(categoriaId: Long, sinceEpochMillis: Long): Double

    @Query("SELECT * FROM gastos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GastoEntity?

    @Query("SELECT * FROM gastos WHERE sincronizado = 0")
    suspend fun getUnsynced(): List<GastoEntity>

    @Query("SELECT * FROM gastos")
    suspend fun getAllOnce(): List<GastoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gasto: GastoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(gastos: List<GastoEntity>)

    @Update
    suspend fun update(gasto: GastoEntity)

    @Delete
    suspend fun delete(gasto: GastoEntity)

    @Query("DELETE FROM gastos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE gastos SET sincronizado = 1, remoteId = :remoteId WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: String)

    @Query("SELECT COUNT(*) FROM gastos")
    suspend fun count(): Int
}
