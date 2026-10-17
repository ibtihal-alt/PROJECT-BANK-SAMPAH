package com.example.sisteminformasiramahlingkungandanmanajemensampah

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FleetRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun updateLocation(
        courierId: String,
        nama: String,
        lat: Double,
        lng: Double,
        kapasitasMaksKg: Double = 500.0,
        muatanSaatIniKg: Double = 0.0,
        jenisKendaraan: String = "Motor Roda Tiga (Tossa)"
    ) {
        val data = mapOf(
            "courierId" to courierId,
            "namaKurir" to nama,
            "latitude" to lat,
            "longitude" to lng,
            "timestamp" to System.currentTimeMillis(),
            "status" to "AKTIF",
            "kapasitasMaksKg" to kapasitasMaksKg,
            "muatanSaatIniKg" to muatanSaatIniKg,
            "jenisKendaraan" to jenisKendaraan
        )
        firestore.collection("fleets").document(courierId).set(data).await()
    }
}
