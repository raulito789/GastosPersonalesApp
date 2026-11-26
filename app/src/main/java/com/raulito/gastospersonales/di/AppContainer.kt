package com.raulito.gastospersonales.di

import android.content.Context
import com.raulito.gastospersonales.data.local.AppDatabase
import com.raulito.gastospersonales.data.remote.NetworkModule
import com.raulito.gastospersonales.data.repository.AuthRepository
import com.raulito.gastospersonales.data.repository.ExpenseRepository
import com.raulito.gastospersonales.notifications.BudgetNotifier
import com.raulito.gastospersonales.security.CryptoManager
import com.raulito.gastospersonales.security.SecureTokenStorage

class AppContainer(context: Context) {
    val cryptoManager = CryptoManager()
    val tokenStorage = SecureTokenStorage(context, cryptoManager)
    val networkModule = NetworkModule(tokenStorage)
    val budgetNotifier = BudgetNotifier(context)

    val database: AppDatabase by lazy {
        val passphrase = tokenStorage.getOrCreateDatabasePassphrase()
        AppDatabase.create(context, passphrase)
    }

    val authRepository by lazy {
        AuthRepository(networkModule, tokenStorage)
    }

    val expenseRepository by lazy {
        ExpenseRepository(
            database = database,
            crypto = cryptoManager,
            tokenStorage = tokenStorage,
            networkModule = networkModule,
            notifier = budgetNotifier
        )
    }
}
