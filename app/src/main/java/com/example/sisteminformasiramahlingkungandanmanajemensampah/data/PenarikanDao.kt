package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PenarikanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(penarikan: PenarikanSaldo): Long

    @Update
    suspend fun update(penarikan: PenarikanSaldo)

    @Query("SELECT * FROM penarikan_saldo WHERE userId = :userId ORDER BY tanggal DESC")
    fun getPenarikanByUser(userId: String): Flow<List<PenarikanSaldo>>

    @Query("SELECT SUM(nominalRp) FROM penarikan_saldo WHERE userId = :userId AND status IN ('MENUNGGU', 'SUKSES')")
    fun getTotalPenarikanByUser(userId: String): Flow<Double?>

    // KHUSUS ADMIN
    @Query("SELECT * FROM penarikan_saldo ORDER BY tanggal DESC")
    fun getAllPenarikanForAdmin(): Flow<List<PenarikanSaldo>>

    @Query("UPDATE penarikan_saldo SET status = :statusBaru WHERE id = :id")
    suspend fun updateStatusPenarikanAdmin(id: Int, statusBaru: String)
}