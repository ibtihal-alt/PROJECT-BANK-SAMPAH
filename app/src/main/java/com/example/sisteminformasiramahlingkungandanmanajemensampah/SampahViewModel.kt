package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.SampahDao
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class SampahViewModel(
    private val sampahDao: SampahDao
) : ViewModel() {

    private val firestoreRepository =
        FirestoreSampahRepository()

    private val auth =
        FirebaseAuth.getInstance()

    // UID pengguna yang sedang login
    private fun getUserId(): String {

        return auth.currentUser?.uid ?: ""
    }

    // =========================
    // DATA USER SAAT INI
    // =========================
    val semuaSampah: Flow<List<Sampah>>
        get() {
            val userId = getUserId()

            return if (userId.isNotEmpty()) {
                sampahDao.getAllSampah(userId)
            } else {
                flowOf(emptyList())
            }
        }

    val totalBerat: Flow<Double>
        get() {
            val userId = getUserId()

            return if (userId.isNotEmpty()) {
                sampahDao.getTotalBerat(userId)
            } else {
                flowOf(0.0)
            }
        }

    val jumlahSampah: Flow<Int>
        get() {
            val userId = getUserId()

            return if (userId.isNotEmpty()) {
                sampahDao.getJumlahSampah(userId)
            } else {
                flowOf(0)
            }
        }

    val jenisTerbanyak: Flow<String?>
        get() {
            val userId = getUserId()

            return if (userId.isNotEmpty()) {
                sampahDao.getJenisTerbanyak(userId)
            } else {
                flowOf(null)
            }
        }
    val aktivitasTerbaru: Flow<List<Sampah>>
        get() {
            val userId = getUserId()

            return if (userId.isNotEmpty()) {
                sampahDao.getAktivitasTerbaru(userId)
            } else {
                flowOf(emptyList())
            }
        }
    // =========================
    // SIMPAN
    // =========================
    fun simpanSampah(
        jenisSampah: String,
        berat: Double,
        catatan: String
    ) {

        val userId = getUserId()

        if (userId.isBlank()) {
            Log.e(
                "SIRAM",
                "Pengguna belum login"
            )
            return
        }

        viewModelScope.launch {

            try {

                val sampah = Sampah(
                    userId = userId,
                    jenisSampah = jenisSampah,
                    berat = berat,
                    catatan = catatan
                )

                // Simpan lokal
                val idBaru =
                    sampahDao.insert(sampah)

                val sampahDenganId =
                    sampah.copy(
                        id = idBaru.toInt()
                    )

                // Simpan ke Firebase
                val firebaseId =
                    firestoreRepository.simpanSampah(
                        sampahDenganId
                    )

                // Simpan ID Firebase ke Room
                sampahDao.updateFirebaseId(
                    id = idBaru.toInt(),
                    firebaseId = firebaseId
                )

            } catch (e: Exception) {

                Log.e(
                    "SIRAM",
                    "Gagal menyimpan data",
                    e
                )
            }
        }
    }

    // =========================
    // UPDATE
    // =========================
    fun updateSampah(
        sampah: Sampah
    ) {

        viewModelScope.launch {

            try {

                sampahDao.update(sampah)

                firestoreRepository
                    .updateSampah(sampah)

            } catch (e: Exception) {

                Log.e(
                    "SIRAM",
                    "Gagal mengupdate data",
                    e
                )
            }
        }
    }

    // =========================
    // HAPUS
    // =========================
    fun hapusSampah(
        sampah: Sampah
    ) {

        viewModelScope.launch {

            try {

                firestoreRepository
                    .hapusSampah(sampah)

                sampahDao.delete(sampah)

            } catch (e: Exception) {

                Log.e(
                    "SIRAM",
                    "Gagal menghapus data",
                    e
                )
            }
        }
    }
}