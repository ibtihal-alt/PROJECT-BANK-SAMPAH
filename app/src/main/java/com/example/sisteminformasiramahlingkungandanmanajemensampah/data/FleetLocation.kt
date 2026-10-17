package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

data class FleetLocation(
    val courierId: String = "",
    val namaKurir: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "AKTIF",
    val kapasitasMaksKg: Double = 500.0,
    val muatanSaatIniKg: Double = 0.0,
    val jenisKendaraan: String = "Motor Roda Tiga (Tossa)",
    val nomorPlat: String = "AE 4821 SIR"
)
