package com.raulito.gastospersonales.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.raulito.gastospersonales.GastosApp
import java.util.concurrent.TimeUnit

/**
 * Scheduled background worker that inspects weekly and monthly budgets
 * and fires local notifications when spending crosses the configured threshold (default 80%).
 */
class BudgetCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GastosApp ?: return Result.failure()
        val alertsSent = app.container.expenseRepository.evaluateBudgetsAndNotify()
        return if (alertsSent >= 0) Result.success() else Result.retry()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "periodic_budget_alert_check"
        private const val IMMEDIATE_WORK_NAME = "immediate_budget_alert_check"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<BudgetCheckWorker>(12, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleImmediateCheck(context: Context, delaySeconds: Long = 3L) {
            val request = OneTimeWorkRequestBuilder<BudgetCheckWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
