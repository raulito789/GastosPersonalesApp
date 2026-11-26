package com.raulito.gastospersonales.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.raulito.gastospersonales.data.local.entity.GastoConCategoria
import com.raulito.gastospersonales.data.local.entity.PresupuestoConCategoria
import com.raulito.gastospersonales.data.repository.ExpenseRepository
import com.raulito.gastospersonales.data.repository.SyncState
import com.raulito.gastospersonales.ui.util.CurrencyFormatter
import com.raulito.gastospersonales.ui.viewmodel.DashboardMetrics

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    presupuestos: List<PresupuestoConCategoria>,
    gastos: List<GastoConCategoria>,
    syncState: SyncState,
    onSyncClick: () -> Unit
) {
    val weekStart = remember { ExpenseRepository.startOfCurrentWeekMillis() }
    val monthStart = remember { ExpenseRepository.startOfCurrentMonthMillis() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SummaryHeaderCard(
                totalSemanal = metrics.totalSemanal,
                totalPresupuestoSemanal = metrics.totalPresupuestoSemanal,
                pendientesSync = metrics.pendientesSync,
                syncState = syncState,
                onSyncClick = onSyncClick
            )
        }

        item {
            WeeklyCategoryChartCard(metrics = metrics)
        }

        item {
            Text(
                text = "Estado de presupuestos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(presupuestos, key = { it.presupuesto.id }) { item ->
            val since = if (item.presupuesto.periodo == "MENSUAL") monthStart else weekStart
            val spent = gastos
                .filter { it.gasto.categoriaId == item.categoria.id && it.gasto.fechaEpochMillis >= since }
                .sumOf { it.gasto.monto }
            val limit = item.presupuesto.montoLimite.coerceAtLeast(1.0)
            val progress = (spent / limit).toFloat().coerceIn(0f, 1f)
            val percent = ((spent / limit) * 100).toInt()
            val isWarning = percent >= item.presupuesto.alertaPorcentaje

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.categoria.nombre} (${item.presupuesto.periodo})",
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isWarning) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Alerta",
                                    tint = if (percent >= 100) Color(0xFFDC2626) else Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = "$percent%",
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    percent >= 100 -> Color(0xFFDC2626)
                                    isWarning -> Color(0xFFD97706)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = when {
                            percent >= 100 -> Color(0xFFDC2626)
                            isWarning -> Color(0xFFD97706)
                            else -> Color(item.categoria.colorHex)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${CurrencyFormatter.format(spent)} de ${CurrencyFormatter.format(item.presupuesto.montoLimite)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryHeaderCard(
    totalSemanal: Double,
    totalPresupuestoSemanal: Double,
    pendientesSync: Int,
    syncState: SyncState,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gasto de esta semana",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = CurrencyFormatter.format(totalSemanal),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (totalPresupuestoSemanal > 0) {
                val disponible = (totalPresupuestoSemanal - totalSemanal).coerceAtLeast(0.0)
                Text(
                    text = "Disponible semanal: ${CurrencyFormatter.format(disponible)} de ${CurrencyFormatter.format(totalPresupuestoSemanal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (syncState) {
                        is SyncState.Loading -> "Sincronizando con nube..."
                        is SyncState.Success -> "Sincronizado (${syncState.syncedCount} mov.)"
                        is SyncState.Error -> syncState.message
                        SyncState.Idle -> "$pendientesSync pendientes de sincronizar"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (syncState is SyncState.Error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onSyncClick,
                    enabled = syncState !is SyncState.Loading
                ) {
                    if (syncState is SyncState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Sincronizar")
                }
            }
        }
    }
}

@Composable
private fun WeeklyCategoryChartCard(metrics: DashboardMetrics) {
    val data = metrics.totalesPorCategoria
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = remember(data) { data.map { it.nombreCategoria.take(6) } }

    LaunchedEffect(data) {
        if (data.isNotEmpty()) {
            val values = data.map { it.total.coerceAtLeast(0.0) }
            modelProducer.runTransaction {
                columnSeries { series(values) }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gastos semanales por categoría (Vico)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (data.isEmpty() || data.all { it.total == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Registra gastos esta semana para ver el gráfico",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val bottomFormatter = CartesianValueFormatter { _, value, _ ->
                    labels.getOrNull(value.toInt()) ?: ""
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter),
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
            }
        }
    }
}
