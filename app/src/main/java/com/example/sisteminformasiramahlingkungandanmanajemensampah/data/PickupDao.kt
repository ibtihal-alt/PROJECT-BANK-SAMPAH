package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PickupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pickup: PickupTransaction): Long

    @Query("SELECT SUM(totalNominalRp) FROM pickup_transactions WHERE userId = :userId AND status = 'SELESAI'")
    fun getTotalSaldoPickupByUser(userId: String): Flow<Double?>

    @Query("SELECT SUM(CASE WHEN beratAktualKg > 0 THEN beratAktualKg ELSE estimasiBeratKg END) FROM pickup_transactions WHERE userId = :userId AND status = 'SELESAI'")
    fun getTotalBeratPickupByUser(userId: String): Flow<Double?>

    @Update
    suspend fun update(pickup: PickupTransaction)

    @Query("SELECT * FROM pickup_transactions WHERE userId = :userId ORDER BY tanggalJemput DESC")
    fun getAllPickupByUser(userId: String): Flow<List<PickupTransaction>>

    @Query("SELECT * FROM pickup_transactions WHERE userId = :userId AND status IN ('MENUNGGU', 'DIPROSES') ORDER BY tanggalJemput DESC")
    fun getActivePickupByUser(userId: String): Flow<List<PickupTransaction>>

    @Query("UPDATE pickup_transactions SET firebaseId = :firebaseId WHERE id = :id")
    suspend fun updateFirebaseId(id: Int, firebaseId: String)

    // ==========================================
    // QUERY KHUSUS ADMIN
    // ==========================================

    @Query("SELECT * FROM pickup_transactions ORDER BY tanggalJemput DESC")
    fun getAllPickupForAdmin(): Flow<List<PickupTransaction>>

    @Query("UPDATE pickup_transactions SET status = :statusBaru WHERE id = :pickupId")
    suspend fun updateStatusPickupAdmin(pickupId: Int, statusBaru: String)
}
