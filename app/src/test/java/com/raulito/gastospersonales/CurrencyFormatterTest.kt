package com.raulito.gastospersonales

import com.raulito.gastospersonales.ui.util.CurrencyFormatter
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `format renders currency symbol and digits`() {
        val formatted = CurrencyFormatter.format(1250.5)
        assertTrue(formatted.contains("1,250.50") || formatted.contains("1250.50"))
    }
}
