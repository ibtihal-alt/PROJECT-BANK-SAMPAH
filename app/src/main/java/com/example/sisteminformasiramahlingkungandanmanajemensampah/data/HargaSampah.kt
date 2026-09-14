package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabel_harga_sampah")
data class HargaSampah(
    @PrimaryKey
    val jenisSampah: String, // Contoh: "Plastik", "Kertas", "Logam", "Kaca"
    val hargaPerKg: Double,
    val updatedAt: Long = System.currentTimeMillis()
)