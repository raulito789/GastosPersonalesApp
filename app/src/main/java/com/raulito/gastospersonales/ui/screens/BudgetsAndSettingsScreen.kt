package com.raulito.gastospersonales.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raulito.gastospersonales.data.local.entity.CategoriaEntity
import com.raulito.gastospersonales.data.local.entity.PresupuestoConCategoria
import com.raulito.gastospersonales.data.local.entity.PresupuestoEntity
import com.raulito.gastospersonales.security.BiometricAuthHelper
import com.raulito.gastospersonales.ui.util.CurrencyFormatter

@Composable
fun BudgetsAndSettingsScreen(
    userEmail: String,
    isLiveSupabase: Boolean,
    cloudConfig: Pair<String, String>,
    biometricEnabled: Boolean,
    presupuestos: List<PresupuestoConCategoria>,
    categorias: List<CategoriaEntity>,
    onSavePresupuesto: (Long, String, String, Int, () -> Unit, (String) -> Unit) -> Unit,
    onDeletePresupuesto: (PresupuestoEntity) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onTriggerNotification: () -> Unit,
    onSaveCloudConfig: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val biometricSupported = remember { BiometricAuthHelper.isBiometricAvailable(context) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var biometricChecked by remember(biometricEnabled) { mutableStateOf(biometricEnabled) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onTriggerNotification()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Presupuestos configurados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = { showBudgetDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Definir")
                }
            }
        }

        items(presupuestos, key = { it.presupuesto.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.categoria.nombre,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Límite ${item.presupuesto.periodo.lowercase()}: ${CurrencyFormatter.format(item.presupuesto.montoLimite)} • Alerta al ${item.presupuesto.alertaPorcentaje}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeletePresupuesto(item.presupuesto) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar presupuesto",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Notificaciones y Seguridad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Bloqueo biométrico", fontWeight = FontWeight.Medium)
                                Text(
                                    if (biometricSupported) "Huella / Face ID al abrir app" else "Sensor no configurado en dispositivo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = biometricChecked,
                            enabled = biometricSupported,
                            onCheckedChange = {
                                biometricChecked = it
                                onToggleBiometric(it)
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onTriggerNotification()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Probar alerta local de presupuesto (>80%)")
                    }

                    OutlinedButton(
                        onClick = { showCloudDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isLiveSupabase) "Supabase en vivo conectado" else "Configurar Supabase (Modo Demo activo)")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Protección activa • $userEmail", fontWeight = FontWeight.SemiBold)
                        Text(
                            "BD SQLCipher AES-256 + Notas y tokens cifrados con Android KeyStore AES-256-GCM.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }

    if (showBudgetDialog) {
        BudgetFormDialog(
            categorias = categorias,
            onDismiss = { showBudgetDialog = false },
            onSave = { catId, rawLim, periodo, alertPct, onErr ->
                onSavePresupuesto(catId, rawLim, periodo, alertPct, { showBudgetDialog = false }, onErr)
            }
        )
    }

    if (showCloudDialog) {
        CloudConfigDialog(
            initialUrl = cloudConfig.first,
            initialKey = cloudConfig.second,
            onDismiss = { showCloudDialog = false },
            onSave = { url, key ->
                onSaveCloudConfig(url, key)
                showCloudDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetFormDialog(
    categorias: List<CategoriaEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, Int, (String) -> Unit) -> Unit
) {
    var selectedCatId by remember { mutableLongStateOf(categorias.firstOrNull()?.id ?: 1L) }
    var limite by remember { mutableStateOf("1500") }
    var periodo by remember { mutableStateOf("SEMANAL") }
    var alertPct by remember { mutableIntStateOf(80) }
    var expanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedCat = categorias.firstOrNull { it.id == selectedCatId } ?: categorias.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCat?.nombre ?: "Categoría",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nombre) },
                                onClick = {
                                    selectedCatId = cat.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limite,
                    onValueChange = {
                        limite = it
                        errorMsg = null
                    },
                    label = { Text("Monto límite (MXN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = periodo == "SEMANAL",
                        onClick = { periodo = "SEMANAL" },
                        label = { Text("Semanal") }
                    )
                    FilterChip(
                        selected = periodo == "MENSUAL",
                        onClick = { periodo = "MENSUAL" },
                        label = { Text("Mensual") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(70, 80, 90).forEach { pct ->
                        FilterChip(
                            selected = alertPct == pct,
                            onClick = { alertPct = pct },
                            label = { Text("Alerta $pct%") }
                        )
                    }
                }

                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Button(onClick = { onSave(selectedCatId, limite, periodo, alertPct) { errorMsg = it } }) {
                    Text("Guardar")
                }
            }
        }
    )
}
