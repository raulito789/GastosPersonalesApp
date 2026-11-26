package com.raulito.gastospersonales.security

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
    val errorOrNull: String? get() = (this as? Invalid)?.message
}

/**
 * Centralized input validation and sanitization to prevent malformed amounts,
 * weak credentials, and script/SQL injection payloads.
 */
object InputValidator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val AMOUNT_REGEX = Regex("^\\d{1,6}(\\.\\d{1,2})?$")
    private val DANGEROUS_PATTERN = Regex("(<script|</script|javascript:|onerror=|onload=|--|;\\s*drop\\s+table)", RegexOption.IGNORE_CASE)

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("El correo electrónico es obligatorio.")
            !EMAIL_REGEX.matches(trimmed) -> ValidationResult.Invalid("Ingresa un correo electrónico válido.")
            else -> ValidationResult.Valid
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.length < 8 -> ValidationResult.Invalid("La contraseña debe tener al menos 8 caracteres.")
            !password.any { it.isUpperCase() } -> ValidationResult.Invalid("Debe incluir al menos una letra mayúscula.")
            !password.any { it.isLowerCase() } -> ValidationResult.Invalid("Debe incluir al menos una letra minúscula.")
            !password.any { it.isDigit() } -> ValidationResult.Invalid("Debe incluir al menos un número.")
            else -> ValidationResult.Valid
        }
    }

    fun validateTitle(title: String): ValidationResult {
        val clean = sanitizeText(title)
        return when {
            clean.length < 2 -> ValidationResult.Invalid("El concepto debe tener al menos 2 caracteres.")
            clean.length > 80 -> ValidationResult.Invalid("El concepto no puede exceder 80 caracteres.")
            DANGEROUS_PATTERN.containsMatchIn(title) -> ValidationResult.Invalid("El texto contiene caracteres no permitidos.")
            else -> ValidationResult.Valid
        }
    }

    fun validateAmount(rawAmount: String): ValidationResult {
        val normalized = rawAmount.trim().replace(',', '.')
        if (normalized.isEmpty()) {
            return ValidationResult.Invalid("El monto es obligatorio.")
        }
        if (!AMOUNT_REGEX.matches(normalized)) {
            return ValidationResult.Invalid("Ingresa un monto válido (máximo 2 decimales, ej. 245.50).")
        }
        val value = normalized.toDoubleOrNull()
            ?: return ValidationResult.Invalid("Formato numérico inválido.")
        return when {
            value <= 0.0 -> ValidationResult.Invalid("El monto debe ser mayor a $0.00.")
            value > 999_999.99 -> ValidationResult.Invalid("El monto excede el límite permitido ($999,999.99).")
            else -> ValidationResult.Valid
        }
    }

    fun validateNote(note: String): ValidationResult {
        return when {
            note.length > 300 -> ValidationResult.Invalid("La nota no puede superar los 300 caracteres.")
            DANGEROUS_PATTERN.containsMatchIn(note) -> ValidationResult.Invalid("La nota contiene secuencias no permitidas.")
            else -> ValidationResult.Valid
        }
    }

    fun validateBudgetLimit(rawAmount: String, alertPercent: Int): ValidationResult {
        val amountResult = validateAmount(rawAmount)
        if (amountResult is ValidationResult.Invalid) return amountResult
        if (alertPercent !in 10..100) {
            return ValidationResult.Invalid("El porcentaje de alerta debe estar entre 10% y 100%.")
        }
        return ValidationResult.Valid
    }

    fun sanitizeText(input: String): String {
        return input.trim()
            .replace(Regex("[\\u0000-\\u001F]"), "")
            .replace(Regex("\\s+"), " ")
    }
}
