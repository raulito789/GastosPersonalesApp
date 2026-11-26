package com.raulito.gastospersonales.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class GastoConCategoria(
    @Embedded val gasto: GastoEntity,
    @Relation(
        parentColumn = "categoriaId",
        entityColumn = "id"
    )
    val categoria: CategoriaEntity
)

data class PresupuestoConCategoria(
    @Embedded val presupuesto: PresupuestoEntity,
    @Relation(
        parentColumn = "categoriaId",
        entityColumn = "id"
    )
    val categoria: CategoriaEntity
)

data class GastoPorCategoria(
    val categoriaId: Long,
    val nombreCategoria: String,
    val colorHex: Long,
    val total: Double
)
