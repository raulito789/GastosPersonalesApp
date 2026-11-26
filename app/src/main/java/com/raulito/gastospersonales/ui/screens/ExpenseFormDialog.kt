package com.raulito.gastospersonales.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.raulito.gastospersonales.data.local.entity.CategoriaEntity
import com.raulito.gastospersonales.data.local.entity.GastoConCategoria
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    editingItem: GastoConCategoria?,
    categorias: List<CategoriaEntity>,
    onDismiss: () -> Unit,
    onSave: (
        editingId: Long,
        titulo: String,
        rawMonto: String,
        categoriaId: Long,
        nota: String,
        rutaTicket: String?,
        onError: (String) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf(editingItem?.gasto?.titulo.orEmpty()) }
    var monto by remember {
        mutableStateOf(editingItem?.gasto?.monto?.toString().orEmpty())
    }
    var nota by remember { mutableStateOf(editingItem?.gasto?.notaCifrada.orEmpty()) }
    var selectedCategoryId by remember {
        mutableLongStateOf(editingItem?.gasto?.categoriaId ?: categorias.firstOrNull()?.id ?: 1L)
    }
    var rutaTicket by remember { mutableStateOf(editingItem?.gasto?.rutaTicket) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        if (captured && !pendingPhotoPath.isNullOrBlank()) {
            rutaTicket = pendingPhotoPath
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (uri, path) = createReceiptFileUri(context)
            pendingPhotoUri = uri
            pendingPhotoPath = path
            takePictureLauncher.launch(uri)
        } else {
            errorMessage = "Se requiere permiso de cámara para capturar el ticket."
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            rutaTicket = copyUriToReceipts(context, uri)
        }
    }

    val selectedCategory = categorias.firstOrNull { it.id == selectedCategoryId } ?: categorias.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingItem == null) "Nuevo gasto" else "Editar gasto") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        errorMessage = null
                    },
                    label = { Text("Concepto") },
                    placeholder = { Text("Ej. Despensa semanal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = monto,
                    onValueChange = {
                        monto = it
                        errorMessage = null
                    },
                    label = { Text("Monto (MXN)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.nombre ?: "Seleccionar categoría",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nombre) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nota,
                    onValueChange = {
                        nota = it
                        errorMessage = null
                    },
                    label = { Text("Nota privada (cifrada con AES-256)") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasCam = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCam) {
                                val (uri, path) = createReceiptFileUri(context)
                                pendingPhotoUri = uri
                                pendingPhotoPath = path
                                takePictureLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cámara")
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Galería")
                    }
                }

                if (!rutaTicket.isNullOrBlank()) {
                    AsyncImage(
                        model = File(rutaTicket!!),
                        contentDescription = "Foto del ticket",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Button(
                    onClick = {
                        onSave(
                            editingItem?.gasto?.id ?: 0L,
                            titulo,
                            monto,
                            selectedCategoryId,
                            nota,
                            rutaTicket
                        ) { err -> errorMessage = err }
                    }
                ) {
                    Text("Guardar")
                }
            }
        }
    )
}

private fun createReceiptFileUri(context: Context): Pair<Uri, String> {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "ticket_${UUID.randomUUID()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return uri to file.absolutePath
}

private fun copyUriToReceipts(context: Context, uri: Uri): String? {
    return runCatching {
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val dest = File(dir, "ticket_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.absolutePath
    }.getOrNull()
}
