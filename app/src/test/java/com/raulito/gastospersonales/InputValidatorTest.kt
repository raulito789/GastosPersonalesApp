package com.raulito.gastospersonales

import com.raulito.gastospersonales.security.InputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    @Test
    fun `valid email and strong password pass validation`() {
        assertTrue(InputValidator.validateEmail("carlos@gastos.mx").isValid)
        assertTrue(InputValidator.validatePassword("Gastos2026!").isValid)
    }

    @Test
    fun `invalid email and weak password are rejected`() {
        assertFalse(InputValidator.validateEmail("correo-invalido").isValid)
        assertFalse(InputValidator.validatePassword("1234").isValid)
        assertFalse(InputValidator.validatePassword("sinmayuscula1").isValid)
    }

    @Test
    fun `amount validation rejects zero negative overflow and excessive decimals`() {
        assertTrue(InputValidator.validateAmount("245.50").isValid)
        assertTrue(InputValidator.validateAmount("120,5").isValid)
        assertFalse(InputValidator.validateAmount("0").isValid)
        assertFalse(InputValidator.validateAmount("-50").isValid)
        assertFalse(InputValidator.validateAmount("12.345").isValid)
        assertFalse(InputValidator.validateAmount("1000000").isValid)
    }

    @Test
    fun `title and note block xss and sql injection patterns`() {
        assertTrue(InputValidator.validateTitle("Café y pan").isValid)
        assertFalse(InputValidator.validateTitle("<script>alert(1)</script>").isValid)
        assertFalse(InputValidator.validateNote("x; DROP TABLE gastos").isValid)
        assertEquals("Hola mundo", InputValidator.sanitizeText("  Hola   mundo \n "))
    }

    @Test
    fun `budget limit requires valid amount and threshold between 10 and 100`() {
        assertTrue(InputValidator.validateBudgetLimit("1500.00", 80).isValid)
        assertFalse(InputValidator.validateBudgetLimit("1500.00", 5).isValid)
        assertFalse(InputValidator.validateBudgetLimit("1500.00", 110).isValid)
    }
}
