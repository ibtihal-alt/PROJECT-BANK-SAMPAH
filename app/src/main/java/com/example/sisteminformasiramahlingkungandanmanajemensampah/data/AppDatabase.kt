package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Sampah::class,
        PickupTransaction::class,
        PenarikanSaldo::class,
        HargaSampah::class // <- WAJIB: Daftarkan entitas ini agar tabel SQLite dibuat
    ],
    version = 8, // Naikkan versi database dari 7 ke 8
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sampahDao(): SampahDao
    abstract fun pickupDao(): PickupDao
    abstract fun penarikanDao(): PenarikanDao
    abstract fun hargaSampahDao(): HargaSampahDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration database versi 3 → 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE sampah
                    ADD COLUMN hargaPerKg REAL NOT NULL DEFAULT 0.0
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE sampah
                    ADD COLUMN totalNilai REAL NOT NULL DEFAULT 0.0
                    """.trimIndent()
                )
            }
        }

        // Migration database versi 5 → 6 (Perbaikan nama tabel & kolom baru)
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pickup_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `firebaseId` TEXT NOT NULL DEFAULT '',
                        `userId` TEXT NOT NULL DEFAULT '',
                        `jenisSampah` TEXT NOT NULL DEFAULT '',
                        `estimasiBeratKg` REAL NOT NULL DEFAULT 0.0,
                        `beratAktualKg` REAL NOT NULL DEFAULT 0.0,
                        `hargaPerKgAktual` REAL NOT NULL DEFAULT 0.0,
                        `totalNominalRp` REAL NOT NULL DEFAULT 0.0,
                        `alamatLengkap` TEXT NOT NULL DEFAULT '',
                        `nomorHp` TEXT NOT NULL DEFAULT '',
                        `tanggalJemput` INTEGER NOT NULL DEFAULT 0,
                        `catatan` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'MENUNGGU'
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "siram_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_5_6)
                    .fallbackToDestructiveMigration() // Reset otomatis database lokal karena ada penambahan tabel baru
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}