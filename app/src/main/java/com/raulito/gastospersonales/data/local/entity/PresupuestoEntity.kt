package com.raulito.gastospersonales.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "presupuestos",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoriaId"], unique = true)]
)
data class PresupuestoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoriaId: Long,
    val montoLimite: Double,
    val periodo: String = "SEMANAL", // "SEMANAL" o "MENSUAL"
    val alertaPorcentaje: Int = 80,
    val activo: Boolean = true
)
