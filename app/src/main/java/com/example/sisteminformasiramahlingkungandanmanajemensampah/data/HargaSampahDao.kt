package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HargaSampahDao {
    @Query("SELECT * FROM tabel_harga_sampah")
    fun getAllHarga(): Flow<List<HargaSampah>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHarga(harga: HargaSampah)

    @Query("SELECT hargaPerKg FROM tabel_harga_sampah WHERE jenisSampah = :jenis")
    suspend fun getHargaByJenis(jenis: String): Double?
}