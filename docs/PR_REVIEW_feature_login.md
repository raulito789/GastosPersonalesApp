# Pull Request Review: `feature/login` -> `develop`

- **Título del PR:** `feat(auth): Implementar autenticación Supabase, almacenamiento cifrado de tokens y desbloqueo biométrico`
- **Rama origen:** `feature/login`
- **Rama destino:** `develop`
- **Autor / Revisor:** `raulito789` (Auto-revisión técnica documentada)

---

## Resumen de Cambios
1. **`security/CryptoManager.kt`**: Implementación de cifrado simétrico `AES/GCM/NoPadding` de 256 bits respaldado por `AndroidKeyStore` (`gastos_master_aes_key`) con IV aleatorio por operación.
2. **`security/SecureTokenStorage.kt`**: Persistencia cifrada de `accessToken`, `refreshToken`, `userId`, `email`, tiempo de expiración (`expiresAtEpochSec`) y passphrase de SQLCipher.
3. **`security/InputValidator.kt`**: Validación estricta de formato de correo electrónico (RFC), fortaleza de contraseña (>= 8 caracteres, mayúscula, minúscula y dígito) y saneamiento de entradas.
4. **`security/BiometricAuthHelper.kt`**: Integración de `BiometricPrompt` con `BIOMETRIC_STRONG | BIOMETRIC_WEAK | DEVICE_CREDENTIAL` para desbloqueo rápido cuando existe sesión válida.
5. **`data/remote/SupabaseApi.kt` & `NetworkModule.kt`**: Cliente Retrofit + OkHttp para `/auth/v1/signup`, `/auth/v1/token?grant_type=password` y `/auth/v1/logout`, con `redactHeader("Authorization")` y `redactHeader("apikey")` en logs HTTP.
6. **`ui/screens/LoginScreen.kt`**: Interfaz Material 3 con pestañas Iniciar sesión / Crear cuenta, estados `Loading`, `Authenticated` y `Error`, diálogo de configuración Supabase y botón biométrico.

---

## Checklist de Revisión de Código
- [x] Los tokens JWT nunca se guardan en texto plano ni se imprimen en Logcat.
- [x] El estado `AuthState.Loading` deshabilita el botón de envío para evitar peticiones duplicadas.
- [x] Los errores HTTP (4xx/5xx) y excepciones de red muestran mensajes claros en UI.
- [x] `FragmentActivity` es utilizada en `MainActivity` para compatibilidad con `BiometricPrompt`.
- [x] Las pruebas unitarias de `InputValidatorTest` cubren correos válidos/inválidos y contraseñas débiles.

## Decisión
**APROBADO (Merged into `develop` con `--no-ff`).**
