# GastOS — Aplicación Móvil de Gestión de Gastos Personales (MVP)

Aplicación Android nativa desarrollada en **Kotlin + Jetpack Compose (Material 3)** para el control seguro de gastos personales, presupuestos y sincronización en la nube.

---

## Stack Tecnológico y Librerías Integradas

| Capa | Tecnología / Librería | Uso en la App |
|---|---|---|
| Lenguaje & UI | Kotlin 2.2.10 + Jetpack Compose BOM 2026.02.01 (Material 3) | Pantallas declarativas, navegación por pestañas y formularios |
| Base de Datos Local | AndroidX Room 2.7.2 (SQLite) + **SQLCipher 4.6.1** | Persistencia relacional cifrada con AES-256 (`gastos_seguros.db`) |
| Servicios Web / Auth | **Retrofit 2.11.0 + OkHttp 4.12.0** (Supabase Auth & PostgREST) | Registro, login JWT, sincronización bidireccional y manejo de estados |
| Gráficos | **Vico (`com.patrykandpatrick.vico:compose-m3:2.1.3`)** | Gráfico de barras semanal por categoría |
| Multimedia / Cámara | **Coil (`io.coil-kt:coil-compose:2.7.0`)** + `TakePicture` + `FileProvider` | Captura y visualización de fotos de tickets/facturas |
| Sensores Biométricos | `androidx.biometric:biometric:1.1.0` | Desbloqueo rápido con huella dactilar / Face ID |
| Notificaciones | `androidx.work:work-runtime-ktx:2.10.0` + `NotificationCompat` | Alertas locales programadas al superar >= 80% del presupuesto |
| Seguridad | Android KeyStore (`AES/GCM/NoPadding` 256-bit) + `InputValidator` | Cifrado de notas, tokens de sesión y contraseña SQLCipher |

---

## Modelo de Datos Relacional (3 Entidades)

1. **`CategoriaEntity` (`categorias`)**: `id`, `nombre`, `icono`, `colorHex`, `esSistema`.
2. **`PresupuestoEntity` (`presupuestos`)**: `id`, `categoriaId` (FK -> `categorias.id`, `ON DELETE CASCADE`, índice único), `montoLimite`, `periodo` (`SEMANAL` / `MENSUAL`), `alertaPorcentaje` (80% por defecto), `activo`.
3. **`GastoEntity` (`gastos`)**: `id`, `titulo`, `monto`, `fechaEpochMillis`, `notaCifrada` (cifrada con AES-256-GCM), `rutaTicket`, `categoriaId` (FK -> `categorias.id`, `ON DELETE RESTRICT`), `usuarioId`, `sincronizado`, `remoteId`, `updatedAt`.

---

## Flujo de Ramas y Control de Versiones (Git Flow)

- **`main`**: Rama estable de producción.
- **`develop`**: Rama de integración continua.
- **`feature/login`**: Autenticación Supabase, almacenamiento seguro de tokens, validación de credenciales y desbloqueo biométrico (integrada a `develop` mediante Pull Request documentado en `docs/PR_REVIEW_feature_login.md`).
- **`feature/expenses-crud`**: Base de datos Room cifrada con SQLCipher, entidades relacionadas, CRUD completo de gastos, cifrado de notas y captura de ticket con cámara.
- **`feature/charts-notifications`**: Gráficos con Vico, presupuestos, notificaciones locales programadas con WorkManager y sincronización REST.
- **`hotfix/input-validation`**: Corrección urgente desde `main` para reforzar validaciones numéricas y saneamiento anti-inyección en `InputValidator`, fusionada en `main` y `develop`.

---

## Instrucciones de Instalación y Ejecución

### Requisitos previos
- **Android Studio** (SDK 36/37, JDK 17+)
- Emulador Android (API 26+) o dispositivo físico con depuración USB

### Pasos de instalación
1. Clona el repositorio:
   ```bash
   git clone https://github.com/raulito789/GastosPersonalesApp.git
   cd GastosPersonalesApp
   ```
2. Configura `local.properties` en la raíz del proyecto:
   ```properties
   sdk.dir=C\:\\Users\\<tu_usuario>\\AppData\\Local\\Android\\Sdk
   SUPABASE_URL=
   SUPABASE_ANON_KEY=
   ```
   - **Modo Demo REST inmediato**: Dejando `SUPABASE_URL` vacío, la app usa un interceptor REST local que simula HTTP 200/201 para probar login, registro y sincronización sin configuración externa.
   - **Modo Supabase en vivo**: Crea un proyecto gratuito en Supabase, ejecuta [`supabase/schema.sql`](supabase/schema.sql) en el SQL Editor y coloca tu `SUPABASE_URL` y `SUPABASE_ANON_KEY` (también puedes cambiarlas desde la pantalla de Login o Ajustes en tiempo de ejecución).
3. Compila y ejecuta pruebas unitarias desde terminal:
   ```bash
   .\gradlew.bat assembleDebug testDebugUnitTest
   ```
4. Instala el APK generado en `app/build/outputs/apk/debug/app-debug.apk` o abre el proyecto directamente en Android Studio y presiona **Run (`Shift+F10`)**.
