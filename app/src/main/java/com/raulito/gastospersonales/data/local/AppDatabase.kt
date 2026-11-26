package com.raulito.gastospersonales.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.raulito.gastospersonales.data.local.dao.CategoriaDao
import com.raulito.gastospersonales.data.local.dao.GastoDao
import com.raulito.gastospersonales.data.local.dao.PresupuestoDao
import com.raulito.gastospersonales.data.local.entity.CategoriaEntity
import com.raulito.gastospersonales.data.local.entity.GastoEntity
import com.raulito.gastospersonales.data.local.entity.PresupuestoEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Encrypted Room SQLite database protected by SQLCipher (AES-256).
 * Contains 3 related entities: CategoriaEntity, PresupuestoEntity, and GastoEntity.
 */
@Database(
    entities = [
        CategoriaEntity::class,
        PresupuestoEntity::class,
        GastoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoriaDao(): CategoriaDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun gastoDao(): GastoDao

    companion object {
        private const val DB_NAME = "gastos_seguros.db"

        fun create(context: Context, passphrase: ByteArray): AppDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        suspend fun seedDefaultsIfEmpty(db: AppDatabase) {
            val catDao = db.categoriaDao()
            if (catDao.count() == 0) {
                val defaults = listOf(
                    CategoriaEntity(id = 1, nombre = "Alimentación", icono = "restaurant", colorHex = 0xFF0F766E),
                    CategoriaEntity(id = 2, nombre = "Transporte", icono = "directions_car", colorHex = 0xFF2563EB),
                    CategoriaEntity(id = 3, nombre = "Hogar", icono = "home", colorHex = 0xFFD97706),
                    CategoriaEntity(id = 4, nombre = "Salud", icono = "medical_services", colorHex = 0xFFDC2626),
                    CategoriaEntity(id = 5, nombre = "Entretenimiento", icono = "movie", colorHex = 0xFF7C3AED),
                    CategoriaEntity(id = 6, nombre = "Educación", icono = "school", colorHex = 0xFF059669)
                )
                catDao.insertAll(defaults)
            }
            val presDao = db.presupuestoDao()
            if (presDao.count() == 0) {
                presDao.insertAll(
                    listOf(
                        PresupuestoEntity(categoriaId = 1, montoLimite = 1500.0, periodo = "SEMANAL", alertaPorcentaje = 80),
                        PresupuestoEntity(categoriaId = 2, montoLimite = 800.0, periodo = "SEMANAL", alertaPorcentaje = 80),
                        PresupuestoEntity(categoriaId = 3, montoLimite = 3500.0, periodo = "MENSUAL", alertaPorcentaje = 80),
                        PresupuestoEntity(categoriaId = 5, montoLimite = 600.0, periodo = "SEMANAL", alertaPorcentaje = 80)
                    )
                )
            }
        }
    }
}
