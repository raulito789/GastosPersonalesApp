package com.raulito.gastospersonales

import android.app.Application
import com.raulito.gastospersonales.di.AppContainer
import com.raulito.gastospersonales.notifications.BudgetCheckWorker

class GastosApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.budgetNotifier.ensureChannel()
        BudgetCheckWorker.schedulePeriodic(this)
    }
}
