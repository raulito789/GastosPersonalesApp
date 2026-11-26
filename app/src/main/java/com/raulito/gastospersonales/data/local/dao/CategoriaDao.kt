package com.raulito.gastospersonales.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raulito.gastospersonales.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun observeAll(): Flow<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias ORDER BY id ASC")
    suspend fun getAllOnce(): List<CategoriaEntity>

    @Query("SELECT * FROM categorias WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CategoriaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoriaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CategoriaEntity): Long

    @Query("SELECT COUNT(*) FROM categorias")
    suspend fun count(): Int
}
