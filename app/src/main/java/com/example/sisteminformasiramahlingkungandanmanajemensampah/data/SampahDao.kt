package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SampahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
        WHERE userId = :userId AND isDeleted = 0
        ORDER BY tanggal DESC
    """)
    fun getAllSampah(
        userId: String
    ): Flow<List<Sampah>>

    @Query("""
        UPDATE sampah
        SET isDeleted = 1
        WHERE id = :id
    """)
    suspend fun softDelete(id: Int)


    // =========================
    // TOTAL BERAT PER USER (HANYA YANG DISETUJUI ADMIN)
    // =========================
    @Query("""
        SELECT COALESCE(SUM(berat), 0)
        FROM sampah
        WHERE userId = :userId AND status = 'DISETUJUI'
    """)
    fun getTotalBerat(
        userId: String
    ): Flow<Double>


    // =========================
    // TOTAL NILAI PER USER (HANYA YANG DISETUJUI ADMIN)
    // =========================
    @Query("""
        SELECT COALESCE(SUM(totalNilai), 0)
        FROM sampah
        WHERE userId = :userId AND status = 'DISETUJUI'
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


    // ==========================================
    // KHUSUS ADMIN: VALIDASI CATAT SAMPAH MANDIRI
    // ==========================================

    // Mengambil SELURUH pencatatan sampah mandiri dari semua nasabah
    @Query("""
        SELECT * FROM sampah
        ORDER BY tanggal DESC
    """)
    fun getAllSampahForAdmin(): Flow<List<Sampah>>

    // Mengubah status validasi catat sampah oleh Admin (DISETUJUI / DITOLAK)
    @Query("""
        UPDATE sampah
        SET status = :statusBaru
        WHERE id = :sampahId
    """)
    suspend fun updateStatusSampahAdmin(
        sampahId: Int,
        statusBaru: String
    )
}