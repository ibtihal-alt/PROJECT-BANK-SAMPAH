package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

data class FleetLocation(
    val courierId: String = "",
    val namaKurir: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "AKTIF"
)
