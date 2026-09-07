package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Sampah::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sampahDao(): SampahDao

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

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "siram_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}