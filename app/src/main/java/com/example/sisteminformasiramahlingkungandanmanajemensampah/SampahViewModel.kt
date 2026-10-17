package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.HargaSampah
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.HargaSampahDao
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupDao
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PenarikanDao
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PenarikanSaldo
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.SampahDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityCriteriaWeights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SampahViewModel(
    private val sampahDao: SampahDao,
    private val pickupDao: PickupDao,
    private val penarikanDao: PenarikanDao,
    private val hargaSampahDao: HargaSampahDao
) : ViewModel() {

    private val firestoreRepository = FirestoreSampahRepository()
    private val firestorePickupRepository = FirestorePickupRepository()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // ====================================================
    // SISTEM PENDUKUNG KEPUTUSAN (SPK) PRIORITAS PENJEMPUTAN
    // ====================================================
    private val _criteriaWeights = MutableStateFlow(PriorityCriteriaWeights())
    val criteriaWeights: StateFlow<PriorityCriteriaWeights> = _criteriaWeights.asStateFlow()

    fun updateCriteriaWeights(newWeights: PriorityCriteriaWeights) {
        _criteriaWeights.value = newWeights
    }

    fun resetCriteriaWeights() {
        _criteriaWeights.value = PriorityCriteriaWeights()
    }

    // =========================
    // HELPER HARGA RIIL & DEFAULT
    // =========================
    suspend fun getHargaRiilByJenis(jenisSampah: String): Double {
        val hargaDariDb = hargaSampahDao.getHargaByJenis(jenisSampah)
        return if (hargaDariDb != null && hargaDariDb > 0.0) {
            hargaDariDb
        } else {
            getHargaDefaultByJenis(jenisSampah)
        }
    }

    fun getHargaDefaultByJenis(jenisSampah: String): Double {
        return when (jenisSampah.lowercase().trim()) {
            "plastik", "botol plastik" -> 5000.0
            "kardus" -> 3000.0
            "kertas" -> 2500.0
            "kaleng" -> 7000.0
            "logam" -> 10000.0
            else -> 2000.0
        }
    }

    // =========================
    // DATA SAMPAH USER SAAT INI
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

    val totalNilai: Flow<Double>
        get() {
            val userId = getUserId()
            return if (userId.isNotEmpty()) {
                combine(
                    sampahDao.getTotalNilai(userId),
                    penarikanDao.getTotalPenarikanByUser(userId)
                ) { totalSampah, totalTarik ->
                    val pendapatan = totalSampah ?: 0.0
                    val potongan = totalTarik ?: 0.0
                    val hasil = pendapatan - potongan
                    if (hasil < 0.0) 0.0 else hasil
                }
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
    // DATA PENJEMPUTAN SAMPAH
    // =========================
    val semuaPenjemputan: Flow<List<PickupTransaction>>
        get() {
            val userId = getUserId()
            return if (userId.isNotEmpty()) {
                pickupDao.getAllPickupByUser(userId)
            } else {
                flowOf(emptyList())
            }
        }

    val penjemputanAktif: Flow<List<PickupTransaction>>
        get() {
            val userId = getUserId()
            return if (userId.isNotEmpty()) {
                pickupDao.getActivePickupByUser(userId)
            } else {
                flowOf(emptyList())
            }
        }

    // =========================
    // DATA PENARIKAN SALDO USER
    // =========================
    val semuaPenarikanUser: Flow<List<PenarikanSaldo>>
        get() {
            val userId = getUserId()
            return if (userId.isNotEmpty()) {
                penarikanDao.getPenarikanByUser(userId)
            } else {
                flowOf(emptyList())
            }
        }

    // =========================
    // KELOLA HARGA SAMPAH (ADMIN & USER)
    // =========================
    val semuaHargaSampah: StateFlow<List<HargaSampah>> = hargaSampahDao.getAllHarga()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateHargaSampah(jenis: String, hargaBaru: Double) {
        viewModelScope.launch {
            try {
                hargaSampahDao.insertOrUpdateHarga(
                    HargaSampah(jenisSampah = jenis.trim(), hargaPerKg = hargaBaru)
                )
            } catch (e: Exception) {
                Log.e("SIRAM", "Gagal memperbarui harga sampah", e)
            }
        }
    }

    // =========================
    // SIMPAN SAMPAH
    // =========================
    fun simpanSampah(
        jenisSampah: String,
        berat: Double,
        catatan: String,
        hargaPerKgCustom: Double? = null
    ) {
        val userId = getUserId()
        if (userId.isBlank()) return

        viewModelScope.launch {
            try {
                val hargaFix = hargaPerKgCustom ?: getHargaRiilByJenis(jenisSampah)
                val totalNilaiFix = berat * hargaFix

                val sampah = Sampah(
                    userId = userId,
                    jenisSampah = jenisSampah.trim(),
                    berat = berat,
                    hargaPerKg = hargaFix,
                    totalNilai = totalNilaiFix,
                    catatan = catatan,
                    status = "MENUNGGU"
                )

                val idBaru = sampahDao.insert(sampah)
                val sampahDenganId = sampah.copy(id = idBaru.toInt())
                val firebaseId = firestoreRepository.simpanSampah(sampahDenganId)

                sampahDao.updateFirebaseId(idBaru.toInt(), firebaseId)
            } catch (e: Exception) {
                Log.e("SIRAM", "Gagal menyimpan data", e)
            }
        }
    }

    // =========================
    // UPDATE & HAPUS SAMPAH
    // =========================
    fun updateSampah(sampah: Sampah) {
        viewModelScope.launch {
            try {
                val totalNilaiFix = sampah.berat * sampah.hargaPerKg
                val sampahUpdated = sampah.copy(totalNilai = totalNilaiFix)

                sampahDao.update(sampahUpdated)
                firestoreRepository.updateSampah(sampahUpdated)
            } catch (e: Exception) {
                Log.e("SIRAM", "Gagal mengupdate data", e)
            }
        }
    }

    fun hapusSampah(sampah: Sampah) {
        viewModelScope.launch {
            try {
                firestoreRepository.hapusSampah(sampah)
                sampahDao.softDelete(sampah.id)
            } catch (e: Exception) {
                Log.e("SIRAM", "Gagal menghapus data", e)
            }
        }
    }

    // =========================
    // AJUKAN PENJEMPUTAN SAMPAH
    // =========================
    fun ajukanPenjemputan(
        jenisSampah: String,
        estimasiBeratKg: Double,
        alamatLengkap: String,
        nomorHp: String,
        catatan: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        tingkatKepenuhan: Int = 80,
        urgensiWarga: String = "SEDANG",
        kapasitasTempatSampahLiter: Double = 100.0,
        jenisWadah: String = "Tong Standar"
    ) {
        val userId = getUserId()
        if (userId.isBlank()) return

        viewModelScope.launch {
            try {
                val pickup = PickupTransaction(
                    userId = userId,
                    jenisSampah = jenisSampah.trim(),
                    estimasiBeratKg = estimasiBeratKg,
                    alamatLengkap = alamatLengkap,
                    nomorHp = nomorHp,
                    tanggalJemput = System.currentTimeMillis(),
                    catatan = catatan,
                    status = "MENUNGGU",
                    latitude = latitude,
                    longitude = longitude,
                    tingkatKepenuhan = tingkatKepenuhan,
                    urgensiWarga = urgensiWarga,
                    kapasitasTempatSampahLiter = kapasitasTempatSampahLiter,
                    jenisWadah = jenisWadah
                )

                val idBaru = pickupDao.insert(pickup)
                val pickupDenganId = pickup.copy(id = idBaru.toInt())
                val firebaseId = firestorePickupRepository.ajukanPenjemputan(pickupDenganId)

                pickupDao.updateFirebaseId(idBaru.toInt(), firebaseId)
            } catch (e: Exception) {
                Log.e("SIRAM", "Gagal mengajukan penjemputan", e)
            }
        }
    }

    // =========================
    // AJUKAN PENCAIRAN / PENARIKAN SALDO
    // =========================
    fun ajukanPenarikanSaldo(
        metode: String,
        nomorTujuan: String,
        namaPemilik: String,
        nominal: Double
    ) {
        val userId = getUserId()
        if (userId.isBlank()) return

        viewModelScope.launch {
            try {
                val penarikan = PenarikanSaldo(
                    userId = userId,
                    metodePenarikan = metode,
                    nomorTujuan = nomorTujuan,
                    namaPemilik = namaPemilik,
                    nominalRp = nominal,
                    status = "MENUNGGU",
                    tanggal = System.currentTimeMillis()
                )
                penarikanDao.insert(penarikan)
            } catch (e: Exception) {
                Log.e("SIRAM", "Gagal mengajukan penarikan", e)
            }
        }
    }

    // =========================
    // KHUSUS ADMIN: VALIDASI PENJEMPUTAN
    // =========================
    val semuaPenjemputanAdmin: Flow<List<PickupTransaction>> = pickupDao.getAllPickupForAdmin()

    fun assignKurirPenjemputan(pickup: PickupTransaction, kurirId: String, kurirName: String) {
        viewModelScope.launch {
            try {
                val updated = pickup.copy(
                    status = "DIPROSES",
                    assignedKurirId = kurirId,
                    assignedKurirName = kurirName
                )
                pickupDao.update(updated)
                firestorePickupRepository.updateStatusPenjemputan(
                    userId = pickup.userId,
                    firebaseId = pickup.firebaseId,
                    statusBaru = "DIPROSES"
                )
            } catch (e: Exception) {
                Log.e("SIRAM_ADMIN", "Gagal menetapkan kurir penjemputan", e)
            }
        }
    }

    fun updateStatusPenjemputanByAdmin(pickup: PickupTransaction, statusBaru: String) {
        viewModelScope.launch {
            try {
                pickupDao.update(pickup.copy(status = statusBaru))
                firestorePickupRepository.updateStatusPenjemputan(
                    userId = pickup.userId,
                    firebaseId = pickup.firebaseId,
                    statusBaru = statusBaru
                )

                if (statusBaru == "SELESAI") {
                    val beratFix = if (pickup.beratAktualKg > 0) pickup.beratAktualKg else pickup.estimasiBeratKg
                    val hargaFix = if (pickup.hargaPerKgAktual > 0) pickup.hargaPerKgAktual else getHargaRiilByJenis(pickup.jenisSampah)
                    val totalFix = if (pickup.totalNominalRp > 0) pickup.totalNominalRp else (beratFix * hargaFix)

                    val sampahBaru = Sampah(
                        userId = pickup.userId,
                        jenisSampah = pickup.jenisSampah,
                        berat = beratFix,
                        hargaPerKg = hargaFix,
                        totalNilai = totalFix,
                        catatan = "Penjemputan Selesai (Alamat: ${pickup.alamatLengkap})",
                        status = "DISETUJUI"
                    )

                    val idBaru = sampahDao.insert(sampahBaru)
                    val firebaseId = firestoreRepository.simpanSampah(sampahBaru.copy(id = idBaru.toInt()))
                    sampahDao.updateFirebaseId(idBaru.toInt(), firebaseId)
                }
            } catch (e: Exception) {
                Log.e("SIRAM_ADMIN", "Gagal mengupdate status penjemputan", e)
            }
        }
    }

    // =========================
    // KHUSUS ADMIN: VALIDASI CATAT SAMPAH MANDIRI
    // =========================
    val semuaSampahAdmin: Flow<List<Sampah>> = sampahDao.getAllSampahForAdmin()

    fun updateStatusSampahMandiriByAdmin(sampah: Sampah, statusBaru: String) {
        viewModelScope.launch {
            try {
                val sampahUpdated = sampah.copy(status = statusBaru)
                sampahDao.updateStatusSampahAdmin(sampah.id, statusBaru)
                firestoreRepository.updateSampah(sampahUpdated)
            } catch (e: Exception) {
                Log.e("SIRAM_ADMIN", "Gagal mengupdate status sampah mandiri", e)
            }
        }
    }

    fun updateStatusSampahMandiriWithDetailsByAdmin(
        sampah: Sampah,
        beratBaru: Double,
        hargaBaru: Double,
        totalBaru: Double,
        statusBaru: String
    ) {
        viewModelScope.launch {
            try {
                val sampahUpdated = sampah.copy(
                    berat = beratBaru,
                    hargaPerKg = hargaBaru,
                    totalNilai = totalBaru,
                    status = statusBaru
                )
                sampahDao.update(sampahUpdated)
                firestoreRepository.updateSampah(sampahUpdated)
            } catch (e: Exception) {
                Log.e("SIRAM_ADMIN", "Gagal mengupdate status & detail sampah mandiri", e)
            }
        }
    }

    // =========================
    // KHUSUS ADMIN: VALIDASI PENARIKAN SALDO
    // =========================
    val semuaPenarikanAdmin: Flow<List<PenarikanSaldo>> = penarikanDao.getAllPenarikanForAdmin()

    fun updateStatusPenarikanByAdmin(penarikan: PenarikanSaldo, statusBaru: String) {
        viewModelScope.launch {
            try {
                penarikanDao.updateStatusPenarikanAdmin(penarikan.id, statusBaru)
            } catch (e: Exception) {
                Log.e("SIRAM_ADMIN", "Gagal mengupdate status penarikan", e)
            }
        }
    }

    fun updateStatusPenarikanWithDetailsByAdmin(
        penarikan: PenarikanSaldo,
        nominalBaru: Double,
        statusBaru: String
    ) {
        viewModelScope.launch {
            try {
                val penarikanUpdated = penarikan.copy(
                    nominalRp = nominalBaru,
                    status = statusBaru
                )
                penarikanDao.update(penarikanUpdated)
            } catch (e: Exception) {
                Log.e("SIRAM_ADMIN", "Gagal mengupdate status & nominal penarikan", e)
            }
        }
    }

    fun registerAdminBaru(
        email: String,
        password: String,
        nama: String,
        role: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authInstance = FirebaseAuth.getInstance()
                val authResult = authInstance.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = authResult.user?.uid ?: ""
                if (uid.isNotEmpty()) {
                    val userData = hashMapOf(
                        "uid" to uid,
                        "email" to email.trim(),
                        "nama" to nama,
                        "role" to role,
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(uid).set(userData).await()
                    onResult(true, "Berhasil mendaftarkan $role ($nama)!")
                } else {
                    onResult(false, "Gagal membuat akun")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun hapusHargaSampah(jenis: String) {
        viewModelScope.launch {
            try {
                hargaSampahDao.deleteHargaByJenis(jenis)
            } catch (e: Exception) {
                Log.e("SIRAM", "Gagal menghapus harga sampah", e)
            }
        }
    }
}