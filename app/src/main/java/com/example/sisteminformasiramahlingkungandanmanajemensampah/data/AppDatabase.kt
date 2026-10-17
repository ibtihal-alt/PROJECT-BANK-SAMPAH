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
        HargaSampah::class
    ],
    version = 11,
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

        // Migration database versi 5 → 6
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

        // Migration database versi 9 → 10 (Penambahan latitude & longitude pada pickup_transactions)
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE pickup_transactions ADD COLUMN latitude REAL NOT NULL DEFAULT 0.0")
                    database.execSQL("ALTER TABLE pickup_transactions ADD COLUMN longitude REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {
                    // ignore jika kolom sudah ada
                }
            }
        }

        // Migration database versi 10 → 11 (Penambahan kolom Adaptive Priority DSS)
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE pickup_transactions ADD COLUMN tingkatKepenuhan INTEGER NOT NULL DEFAULT 80")
                    database.execSQL("ALTER TABLE pickup_transactions ADD COLUMN urgensiWarga TEXT NOT NULL DEFAULT 'SEDANG'")
                    database.execSQL("ALTER TABLE pickup_transactions ADD COLUMN kapasitasTempatSampahLiter REAL NOT NULL DEFAULT 100.0")
                    database.execSQL("ALTER TABLE pickup_transactions ADD COLUMN jenisWadah TEXT NOT NULL DEFAULT 'Tong Standar'")
                } catch (e: Exception) {
                    // ignore jika kolom sudah ada
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "siram_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_5_6, MIGRATION_9_10, MIGRATION_10_11)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}