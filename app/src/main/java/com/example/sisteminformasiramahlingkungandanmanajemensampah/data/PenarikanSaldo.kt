package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "penarikan_saldo")
data class PenarikanSaldo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firebaseId: String = "",
    val userId: String = "",
    val metodePenarikan: String = "", // Gopay, DANA, OVO, BCA, dll
    val nomorTujuan: String = "",
    val namaPemilik: String = "",
    val nominalRp: Double = 0.0,
    val status: String = "MENUNGGU", // MENUNGGU, SUKSES, DITOLAK
    val tanggal: Long = System.currentTimeMillis()
)