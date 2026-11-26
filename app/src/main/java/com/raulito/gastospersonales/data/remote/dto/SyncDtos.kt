package com.raulito.gastospersonales.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RemoteGastoDto(
    val id: String? = null,
    @SerializedName("local_id") val localId: Long,
    @SerializedName("user_id") val userId: String,
    val titulo: String,
    val monto: Double,
    @SerializedName("fecha_epoch_millis") val fechaEpochMillis: Long,
    @SerializedName("categoria_id") val categoriaId: Long,
    @SerializedName("updated_at") val updatedAt: Long
)

data class RemotePresupuestoDto(
    val id: String? = null,
    @SerializedName("user_id") val userId: String,
    @SerializedName("categoria_id") val categoriaId: Long,
    @SerializedName("monto_limite") val montoLimite: Double,
    val periodo: String,
    @SerializedName("alerta_porcentaje") val alertaPorcentaje: Int
)
