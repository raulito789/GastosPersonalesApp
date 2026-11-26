package com.raulito.gastospersonales.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.raulito.gastospersonales.MainActivity
import com.raulito.gastospersonales.R
import com.raulito.gastospersonales.ui.util.CurrencyFormatter

/**
 * Manages NotificationChannel creation and budget threshold local alerts.
 */
class BudgetNotifier(private val context: Context) {

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de presupuesto",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifica cuando superas el porcentaje configurado de tu presupuesto semanal o mensual."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyBudgetExceeded(
        categoryName: String,
        period: String,
        spent: Double,
        limit: Double,
        alertPercent: Int,
        notificationId: Int = categoryName.hashCode()
    ): Boolean {
        if (!hasNotificationPermission()) return false
        ensureChannel()

        val ratio = if (limit > 0) ((spent / limit) * 100).toInt() else 100
        val periodoLabel = if (period == "SEMANAL") "semanal" else "mensual"
        val title = if (ratio >= 100) {
            "Presupuesto $periodoLabel agotado ($ratio%)"
        } else {
            "Has superado el $alertPercent% de tu presupuesto $periodoLabel"
        }
        val body = "Categoría $categoryName: ${CurrencyFormatter.format(spent)} de ${CurrencyFormatter.format(limit)} ($ratio%)."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }

    companion object {
        const val CHANNEL_ID = "budget_alerts_channel"
    }
}
