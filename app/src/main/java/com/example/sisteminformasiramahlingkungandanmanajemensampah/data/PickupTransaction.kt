package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pickup_transactions")
data class PickupTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firebaseId: String = "",
    val userId: String = "",
    val jenisSampah: String = "",
    val estimasiBeratKg: Double = 0.0,
    val beratAktualKg: Double = 0.0,
    val hargaPerKgAktual: Double = 0.0,
    val totalNominalRp: Double = 0.0,
    val alamatLengkap: String = "",
    val nomorHp: String = "",
    val tanggalJemput: Long = System.currentTimeMillis(),
    val catatan: String = "",
    val status: String = "MENUNGGU",
    val assignedKurirId: String = "",
    val assignedKurirName: String = "",
    val fotoBuktiUri: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // ADAPTIVE CONTEXT-AWARE DSS FIELDS
    val tingkatKepenuhan: Int = 80, // Persentase kepenuhan tempat sampah (10-100%)
    val urgensiWarga: String = "SEDANG", // RENDAH, SEDANG, TINGGI, DARURAT
    val kapasitasTempatSampahLiter: Double = 100.0, // Estimasi kapasitas wadah tempat sampah dalam liter
    val jenisWadah: String = "Tong Standar" // Tong Standar, Kantong Plastik, Karung, Kontainer Komunal
)