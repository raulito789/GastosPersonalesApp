package com.raulito.gastospersonales.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gastos",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoriaId"]),
        Index(value = ["fechaEpochMillis"]),
        Index(value = ["sincronizado"])
    ]
)
data class GastoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val monto: Double,
    val fechaEpochMillis: Long,
    val notaCifrada: String = "",
    val rutaTicket: String? = null,
    val categoriaId: Long,
    val usuarioId: String = "local_user",
    val sincronizado: Boolean = false,
    val remoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
