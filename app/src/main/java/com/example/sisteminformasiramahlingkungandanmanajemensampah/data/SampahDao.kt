package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SampahDao {

    @Insert
    suspend fun insert(sampah: Sampah): Long

    @Update
    suspend fun update(sampah: Sampah)

    @Delete
    suspend fun delete(sampah: Sampah)


    // =========================
    // DATA BERDASARKAN USER
    // =========================
    @Query("""
        SELECT * FROM sampah
        WHERE userId = :userId
        ORDER BY tanggal DESC
    """)
    fun getAllSampah(
        userId: String
    ): Flow<List<Sampah>>


    // =========================
    // TOTAL BERAT PER USER
    // =========================
    @Query("""
        SELECT COALESCE(SUM(berat), 0)
        FROM sampah
        WHERE userId = :userId
    """)
    fun getTotalBerat(
        userId: String
    ): Flow<Double>


    // =========================
    // TOTAL NILAI PER USER
    // =========================
    @Query("""
        SELECT COALESCE(SUM(totalNilai), 0)
        FROM sampah
        WHERE userId = :userId
    """)
    fun getTotalNilai(
        userId: String
    ): Flow<Double>


    // =========================
    // JUMLAH DATA PER USER
    // =========================
    @Query("""
        SELECT COUNT(*)
        FROM sampah
        WHERE userId = :userId
    """)
    fun getJumlahSampah(
        userId: String
    ): Flow<Int>


    // =========================
    // JENIS TERBANYAK PER USER
    // =========================
    @Query("""
        SELECT jenisSampah
        FROM sampah
        WHERE userId = :userId
        GROUP BY jenisSampah
        ORDER BY SUM(berat) DESC
        LIMIT 1
    """)
    fun getJenisTerbanyak(
        userId: String
    ): Flow<String?>


    // =========================
    // AKTIVITAS TERBARU
    // =========================
    @Query("""
        SELECT * FROM sampah
        WHERE userId = :userId
        ORDER BY tanggal DESC
        LIMIT 3
    """)
    fun getAktivitasTerbaru(
        userId: String
    ): Flow<List<Sampah>>


    // =========================
    // SIMPAN FIREBASE ID
    // =========================
    @Query("""
        UPDATE sampah
        SET firebaseId = :firebaseId
        WHERE id = :id
    """)
    suspend fun updateFirebaseId(
        id: Int,
        firebaseId: String
    )
}