package com.raulito.gastospersonales

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raulito.gastospersonales.data.local.entity.GastoConCategoria
import com.raulito.gastospersonales.data.repository.AuthState
import com.raulito.gastospersonales.ui.screens.BudgetsAndSettingsScreen
import com.raulito.gastospersonales.ui.screens.DashboardScreen
import com.raulito.gastospersonales.ui.screens.ExpenseFormDialog
import com.raulito.gastospersonales.ui.screens.ExpensesScreen
import com.raulito.gastospersonales.ui.screens.LoginScreen
import com.raulito.gastospersonales.ui.theme.GastOSTheme
import com.raulito.gastospersonales.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GastosApp).container

        setContent {
            GastOSTheme {
                val vm: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(
                        authRepository = container.authRepository,
                        expenseRepository = container.expenseRepository
                    )
                )
                GastOSRoot(vm = vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GastOSRoot(vm: MainViewModel) {
    val authState by vm.authState.collectAsStateWithLifecycle()
    val banner by vm.statusBanner.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(banner) {
        if (!banner.isNullOrBlank()) {
            snackbarHostState.showSnackbar(banner!!)
            vm.clearBanner()
        }
    }

    when (val state = authState) {
        is AuthState.Authenticated -> {
            var currentTab by remember { mutableIntStateOf(0) }
            var showExpenseDialog by remember { mutableStateOf(false) }
            var editingExpense by remember { mutableStateOf<GastoConCategoria?>(null) }

            val categorias by vm.categorias.collectAsStateWithLifecycle()
            val gastos by vm.gastos.collectAsStateWithLifecycle()
            val presupuestos by vm.presupuestos.collectAsStateWithLifecycle()
            val metrics by vm.dashboardMetrics.collectAsStateWithLifecycle()
            val syncState by vm.syncState.collectAsStateWithLifecycle()
            val selectedCategory by vm.selectedCategoryFilter.collectAsStateWithLifecycle()
            val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentTab) {
                                    0 -> "GastOS • Resumen"
                                    1 -> "GastOS • Gastos"
                                    else -> "GastOS • Presupuestos"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                },
                floatingActionButton = {
                    if (currentTab != 2) {
                        FloatingActionButton(
                            onClick = {
                                editingExpense = null
                                showExpenseDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar gasto")
                        }
                    }
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                            label = { Text("Resumen") }
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                            label = { Text("Gastos") }
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                            label = { Text("Ajustes") }
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentTab) {
                        0 -> DashboardScreen(
                            metrics = metrics,
                            presupuestos = presupuestos,
                            gastos = gastos,
                            syncState = syncState,
                            onSyncClick = vm::syncCloud
                        )
                        1 -> ExpensesScreen(
                            gastos = gastos,
                            categorias = categorias,
                            selectedCategoryId = selectedCategory,
                            searchQuery = searchQuery,
                            onSearchChange = vm::setSearchQuery,
                            onCategoryFilterChange = vm::setCategoryFilter,
                            onEditGasto = { item ->
                                editingExpense = item
                                showExpenseDialog = true
                            },
                            onDeleteGasto = vm::deleteGasto
                        )
                        else -> BudgetsAndSettingsScreen(
                            userEmail = state.email,
                            isLiveSupabase = vm.isLiveSupabaseConfigured(),
                            cloudConfig = vm.getCloudConfig(),
                            biometricEnabled = vm.isBiometricEnabled(),
                            presupuestos = presupuestos,
                            categorias = categorias,
                            onSavePresupuesto = vm::savePresupuesto,
                            onDeletePresupuesto = vm::deletePresupuesto,
                            onToggleBiometric = vm::setBiometricEnabled,
                            onTriggerNotification = vm::triggerBudgetCheckNotification,
                            onSaveCloudConfig = vm::saveCloudConfig,
                            onSignOut = vm::signOut
                        )
                    }
                }
            }

            if (showExpenseDialog) {
                ExpenseFormDialog(
                    editingItem = editingExpense,
                    categorias = categorias,
                    onDismiss = { showExpenseDialog = false },
                    onSave = { editId, titulo, rawMonto, catId, nota, ticket, onErr ->
                        vm.saveGasto(
                            editingId = editId,
                            titulo = titulo,
                            rawMonto = rawMonto,
                            categoriaId = catId,
                            nota = nota,
                            rutaTicket = ticket,
                            onSuccess = { showExpenseDialog = false },
                            onError = onErr
                        )
                    }
                )
            }
        }
        else -> {
            LoginScreen(
                authState = state,
                hasSavedBiometricSession = vm.hasSavedBiometricSession(),
                isLiveSupabase = vm.isLiveSupabaseConfigured(),
                initialCloudConfig = vm.getCloudConfig(),
                onSignIn = vm::signIn,
                onSignUp = vm::signUp,
                onBiometricUnlock = vm::unlockWithBiometricSession,
                onSaveCloudConfig = vm::saveCloudConfig,
                onClearError = vm::clearAuthError
            )
        }
    }
}
