package com.raulito.gastospersonales.ui.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CurrencyFormatter {
    private val currencyFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    }

    private val dateFormat: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale("es", "MX"))
    }

    fun format(amount: Double): String = currencyFormat.format(amount)

    fun formatDate(epochMillis: Long): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        return dateFormat.format(zdt)
    }
}
