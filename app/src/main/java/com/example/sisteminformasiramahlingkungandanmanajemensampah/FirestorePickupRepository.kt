package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.util.Log
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestorePickupRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("Pengguna belum login")
    }

    private fun pickupCollection() =
        firestore.collection("users").document(getUserId()).collection("pickup_requests")

    suspend fun ajukanPenjemputan(pickup: PickupTransaction): String {
        try {
            val data = hashMapOf(
                "jenisSampah" to pickup.jenisSampah,
                "estimasiBeratKg" to pickup.estimasiBeratKg,
                "alamatLengkap" to pickup.alamatLengkap,
                "nomorHp" to pickup.nomorHp,
                "tanggalJemput" to pickup.tanggalJemput,
                "catatan" to pickup.catatan,
                "status" to pickup.status,
                "latitude" to pickup.latitude,
                "longitude" to pickup.longitude,
                "createdAt" to pickup.tanggalJemput,
                "tingkatKepenuhan" to pickup.tingkatKepenuhan,
                "urgensiWarga" to pickup.urgensiWarga,
                "kapasitasTempatSampahLiter" to pickup.kapasitasTempatSampahLiter,
                "jenisWadah" to pickup.jenisWadah
            )

            val docRef = pickupCollection().add(data).await()
            return docRef.id
        } catch (e: Exception) {
            Log.e("FIREBASE_SIRAM", "Gagal menyimpan request penjemputan", e)
            throw e
        }
    }

    // ==========================================
    // KHUSUS ADMIN: UPDATE STATUS PENJEMPUTAN
    // ==========================================
    suspend fun updateStatusPenjemputan(
        userId: String,
        firebaseId: String,
        statusBaru: String
    ) {
        try {
            if (userId.isNotEmpty() && firebaseId.isNotEmpty()) {
                firestore.collection("users")
                    .document(userId)
                    .collection("pickup_requests")
                    .document(firebaseId)
                    .update("status", statusBaru)
                    .await()
            }
        } catch (e: Exception) {
            Log.e("FIREBASE_SIRAM", "Gagal update status penjemputan di Firestore", e)
        }
    }
}