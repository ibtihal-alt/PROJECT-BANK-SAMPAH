package com.example.sisteminformasiramahlingkungandanmanajemensampah

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FleetRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun updateLocation(courierId: String, nama: String, lat: Double, lng: Double) {
        val data = mapOf(
            "courierId" to courierId,
            "namaKurir" to nama,
            "latitude" to lat,
            "longitude" to lng,
            "timestamp" to System.currentTimeMillis(),
            "status" to "AKTIF"
        )
        firestore.collection("fleets").document(courierId).set(data).await()
    }
}
