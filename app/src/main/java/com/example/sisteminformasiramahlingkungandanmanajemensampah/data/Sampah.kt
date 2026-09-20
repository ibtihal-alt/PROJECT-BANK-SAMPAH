package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sampah")
data class Sampah(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val firebaseId: String = "",

    // UID pengguna pemilik data
    val userId: String = "",

    val jenisSampah: String,

    val berat: Double,

    // Harga sampah saat transaksi dicatat
    val hargaPerKg: Double = 0.0,

    // Total nilai = berat × harga per kg
    val totalNilai: Double = 0.0,

    val catatan: String,

    val tanggal: Long = System.currentTimeMillis(),

    // Field Baru untuk Validasi Admin (MENUNGGU, DISETUJUI, DITOLAK)
    val status: String = "MENUNGGU",

    val isDeleted: Boolean = false
)