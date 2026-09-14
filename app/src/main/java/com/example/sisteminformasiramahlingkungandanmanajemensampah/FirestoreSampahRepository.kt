package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.util.Log
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreSampahRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // =========================
    // AMBIL UID PENGGUNA
    // =========================
    private fun getUserId(): String {
        return auth.currentUser?.uid
            ?: throw Exception("Pengguna belum login")
    }

    // =========================
    // PATH SAMPAH PENGGUNA
    // users/{uid}/sampah
    // =========================
    private fun sampahCollection() =
        firestore
            .collection("users")
            .document(getUserId())
            .collection("sampah")

    // =========================
    // SIMPAN DATA
    // =========================
    suspend fun simpanSampah(sampah: Sampah): String {
        try {
            val dataSampah = hashMapOf(
                "jenisSampah" to sampah.jenisSampah,
                "berat" to sampah.berat,
                "hargaPerKg" to sampah.hargaPerKg,   // DITAMBAHKAN
                "totalNilai" to sampah.totalNilai,   // DITAMBAHKAN
                "catatan" to sampah.catatan,
                "tanggal" to sampah.tanggal
            )

            val documentReference =
                sampahCollection()
                    .add(dataSampah)
                    .await()

            Log.d(
                "FIREBASE_SIRAM",
                "Data berhasil disimpan untuk user: ${getUserId()}"
            )

            return documentReference.id

        } catch (e: Exception) {
            Log.e(
                "FIREBASE_SIRAM",
                "Gagal menyimpan data",
                e
            )
            throw e
        }
    }

    // =========================
    // UPDATE DATA
    // =========================
    suspend fun updateSampah(sampah: Sampah) {
        try {
            if (sampah.firebaseId.isBlank()) {
                throw Exception(
                    "Firebase ID tidak ditemukan"
                )
            }

            val dataSampah = hashMapOf(
                "jenisSampah" to sampah.jenisSampah,
                "berat" to sampah.berat,
                "hargaPerKg" to sampah.hargaPerKg,   // DITAMBAHKAN
                "totalNilai" to sampah.totalNilai,   // DITAMBAHKAN
                "catatan" to sampah.catatan,
                "tanggal" to sampah.tanggal
            )

            sampahCollection()
                .document(sampah.firebaseId)
                .set(dataSampah)
                .await()

            Log.d(
                "FIREBASE_SIRAM",
                "Data berhasil diupdate"
            )

        } catch (e: Exception) {
            Log.e(
                "FIREBASE_SIRAM",
                "Gagal update Firebase",
                e
            )
            throw e
        }
    }

    // =========================
    // HAPUS DATA
    // =========================
    suspend fun hapusSampah(sampah: Sampah) {
        try {
            if (sampah.firebaseId.isNotBlank()) {
                sampahCollection()
                    .document(sampah.firebaseId)
                    .delete()
                    .await()

                Log.d(
                    "FIREBASE_SIRAM",
                    "Data berhasil dihapus dari Firebase"
                )
            }

        } catch (e: Exception) {
            Log.e(
                "FIREBASE_SIRAM",
                "Gagal menghapus data dari Firebase",
                e
            )
            throw e
        }
    }
}