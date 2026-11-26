package com.raulito.gastospersonales.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.raulito.gastospersonales.data.repository.AuthState
import com.raulito.gastospersonales.security.BiometricAuthHelper
import com.raulito.gastospersonales.security.InputValidator

@Composable
fun LoginScreen(
    authState: AuthState,
    hasSavedBiometricSession: Boolean,
    isLiveSupabase: Boolean,
    initialCloudConfig: Pair<String, String>,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onBiometricUnlock: () -> Unit,
    onSaveCloudConfig: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = remember { BiometricAuthHelper.isBiometricAvailable(context) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Iniciar sesión, 1 = Registrarse
    var email by remember { mutableStateOf("carlos@gastos.mx") }
    var password by remember { mutableStateOf("Gastos2026!") }
    var showPassword by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }

    val emailValidation = InputValidator.validateEmail(email)
    val passValidation = InputValidator.validatePassword(password)
    val isLoading = authState is AuthState.Loading

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "GastOS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Gestión segura de gastos personales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            AssistChip(
                onClick = { showCloudDialog = true },
                label = {
                    Text(
                        if (isLiveSupabase) "Supabase Auth en vivo" else "Modo Demo REST + SQLCipher"
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                onClearError()
                            },
                            text = { Text("Iniciar sesión") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                onClearError()
                            },
                            text = { Text("Crear cuenta") }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            onClearError()
                        },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        isError = email.isNotBlank() && !emailValidation.isValid,
                        supportingText = {
                            if (email.isNotBlank() && !emailValidation.isValid) {
                                Text(emailValidation.errorOrNull.orEmpty())
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onClearError()
                        },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Mostrar contraseña"
                                )
                            }
                        },
                        isError = password.isNotBlank() && !passValidation.isValid,
                        supportingText = {
                            if (password.isNotBlank() && !passValidation.isValid) {
                                Text(passValidation.errorOrNull.orEmpty())
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (authState is AuthState.Error) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = authState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!biometricError.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = biometricError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (selectedTab == 0) onSignIn(email, password) else onSignUp(email, password)
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Autenticando...")
                        } else {
                            Text(if (selectedTab == 0) "Entrar" else "Registrarme")
                        }
                    }

                    if (hasSavedBiometricSession && biometricAvailable && activity != null) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                biometricError = null
                                BiometricAuthHelper.authenticate(
                                    activity = activity,
                                    onSuccess = onBiometricUnlock,
                                    onError = { biometricError = it }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Desbloquear con huella / Face ID")
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Base local cifrada con SQLCipher + Android KeyStore AES-256-GCM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showCloudDialog) {
        CloudConfigDialog(
            initialUrl = initialCloudConfig.first,
            initialKey = initialCloudConfig.second,
            onDismiss = { showCloudDialog = false },
            onSave = { url, key ->
                onSaveCloudConfig(url, key)
                showCloudDialog = false
            }
        )
    }
}

@Composable
fun CloudConfigDialog(
    initialUrl: String,
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var key by remember { mutableStateOf(initialKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Supabase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Deja los campos vacíos para usar el Modo Demo REST local, o ingresa tu URL y Anon Key de Supabase.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("SUPABASE_URL") },
                    placeholder = { Text("https://xxxxx.supabase.co") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("SUPABASE_ANON_KEY") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Button(onClick = { onSave(url, key) }) { Text("Guardar") }
            }
        }
    )
}
