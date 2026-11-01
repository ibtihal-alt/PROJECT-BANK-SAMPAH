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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val firestorePenarikanRepository = FirestorePenarikanRepository()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // =========================
    // SINKRON FIRESTORE -> ROOM (REAL TIME ANTAR HP): PENJEMPUTAN, SETOR SAMPAH/SALDO, PENARIKAN
    // =========================
    private val syncRegistrations = mutableListOf<ListenerRegistration>()
    private var syncKey = ""
    private val syncMutex = Mutex()

    fun startSync(asStaff: Boolean) {
        val key = "${getUserId()}:$asStaff"
        // Kembali ke dashboard tidak perlu memasang ulang listener; ganti akun/role baru memasang ulang
        if (getUserId().isBlank() || key == syncKey) return
        syncKey = key
        syncRegistrations.forEach { it.remove() }
        syncRegistrations.clear()

        syncRegistrations += firestorePickupRepository.listenPickups(asStaff) { docs ->
            syncLocked {
                docs.forEach { doc ->
                    val local = pickupDao.getByFirebaseId(doc.id)
                    pickupDao.insert(FirestorePickupRepository.fromDoc(doc, local ?: PickupTransaction()))
                }
            }
        }
        syncRegistrations += firestoreRepository.listenSampah(asStaff) { docs ->
            syncLocked {
                docs.forEach { doc ->
                    val local = sampahDao.getByFirebaseId(doc.id)
                    sampahDao.insert(FirestoreSampahRepository.fromDoc(doc, local ?: Sampah(jenisSampah = "", berat = 0.0, catatan = "")))
                }
            }
        }
        syncRegistrations += firestorePenarikanRepository.listenPenarikan(asStaff) { docs ->
            syncLocked {
                docs.forEach { doc ->
                    val local = penarikanDao.getByFirebaseId(doc.id)
                    penarikanDao.insert(FirestorePenarikanRepository.fromDoc(doc, local ?: PenarikanSaldo()))
                }
            }
        }
    }

    // Mutex: dua snapshot beruntun tidak boleh sama-sama menyisipkan baris baru untuk dokumen yang sama
    private fun syncLocked(block: suspend () -> Unit) {
        viewModelScope.launch { syncMutex.withLock { block() } }
    }

    override fun onCleared() {
        syncRegistrations.forEach { it.remove() }
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
                    firebaseId = firestoreRepository.newSampahId(userId),
                    userId = userId,
                    jenisSampah = jenisSampah.trim(),
                    berat = berat,
                    hargaPerKg = hargaFix,
                    totalNilai = totalNilaiFix,
                    catatan = catatan,
                    status = "MENUNGGU"
                )

                sampahDao.insert(sampah)
                firestoreRepository.simpanSampah(sampah)
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
                firestoreRepository.simpanSampah(sampahUpdated)
            } catch (e: Exception) {
                Log.e("SIRAM", "Gagal mengupdate data", e)
            }
        }
    }

    fun hapusSampah(sampah: Sampah) {
        viewModelScope.launch {
            try {
                // Soft delete juga di Firestore agar HP lain ikut menghapus lewat sinkron
                sampahDao.softDelete(sampah.id)
                firestoreRepository.simpanSampah(sampah.copy(isDeleted = true))
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
        jenisWadah: String = "Tong Standar",
        fotoSampah: ByteArray
    ) {
        val userId = getUserId()
        if (userId.isBlank()) return

        viewModelScope.launch {
            try {
                val pickup = PickupTransaction(
                    // ID Firestore dibuat dulu agar listener sinkron mengenali baris lokal ini (tidak dobel)
                    firebaseId = firestorePickupRepository.newPickupId(userId),
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

                pickupDao.insert(pickup)
                // Foto diunggah lebih dulu agar admin tidak pernah melihat pengajuan tanpa foto
                firestorePickupRepository.uploadFoto(pickup.firebaseId, FOTO_NASABAH, fotoSampah)
                firestorePickupRepository.simpanPickup(pickup)
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
                    firebaseId = firestorePenarikanRepository.newPenarikanId(userId),
                    userId = userId,
                    metodePenarikan = metode,
                    nomorTujuan = nomorTujuan,
                    namaPemilik = namaPemilik,
                    nominalRp = nominal,
                    status = "MENUNGGU",
                    tanggal = System.currentTimeMillis()
                )
                penarikanDao.insert(penarikan)
                firestorePenarikanRepository.simpanPenarikan(penarikan)
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
                firestorePickupRepository.simpanPickup(updated)
            } catch (e: Exception) {
                Log.e("SIRAM_ADMIN", "Gagal menetapkan kurir penjemputan", e)
            }
        }
    }

    fun updateStatusPenjemputanByAdmin(pickup: PickupTransaction, statusBaru: String) {
        viewModelScope.launch {
            try {
                val updated = pickup.copy(status = statusBaru)
                pickupDao.update(updated)
                firestorePickupRepository.simpanPickup(updated)

                if (statusBaru == "SELESAI") {
                    val beratFix = if (pickup.beratAktualKg > 0) pickup.beratAktualKg else pickup.estimasiBeratKg
                    val hargaFix = if (pickup.hargaPerKgAktual > 0) pickup.hargaPerKgAktual else getHargaRiilByJenis(pickup.jenisSampah)
                    val totalFix = if (pickup.totalNominalRp > 0) pickup.totalNominalRp else (beratFix * hargaFix)

                    val sampahBaru = Sampah(
                        // ID tetap per penjemputan: verifikasi SELESAI yang terulang tidak menambah saldo dua kali
                        firebaseId = if (pickup.firebaseId.isNotBlank()) "pickup_${pickup.firebaseId}" else "",
                        userId = pickup.userId,
                        jenisSampah = pickup.jenisSampah,
                        berat = beratFix,
                        hargaPerKg = hargaFix,
                        totalNilai = totalFix,
                        catatan = "Penjemputan Selesai (Alamat: ${pickup.alamatLengkap})",
                        status = "DISETUJUI"
                    )

                    val existing = if (sampahBaru.firebaseId.isNotBlank()) sampahDao.getByFirebaseId(sampahBaru.firebaseId) else null
                    sampahDao.insert(sampahBaru.copy(id = existing?.id ?: 0))
                    firestoreRepository.simpanSampah(sampahBaru)
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
                sampahDao.update(sampahUpdated)
                firestoreRepository.simpanSampah(sampahUpdated)
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
                firestoreRepository.simpanSampah(sampahUpdated)
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
                val penarikanUpdated = penarikan.copy(status = statusBaru)
                penarikanDao.update(penarikanUpdated)
                firestorePenarikanRepository.simpanPenarikan(penarikanUpdated)
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
                firestorePenarikanRepository.simpanPenarikan(penarikanUpdated)
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
                // Pakai FirebaseApp kedua: createUser di instance utama akan mengganti sesi super admin ke akun baru
                val defaultApp = FirebaseApp.getInstance()
                val creatorApp = FirebaseApp.getApps(defaultApp.applicationContext).firstOrNull { it.name == "pembuat_akun" }
                    ?: FirebaseApp.initializeApp(defaultApp.applicationContext, defaultApp.options, "pembuat_akun")
                val authInstance = FirebaseAuth.getInstance(creatorApp)
                val authResult = authInstance.createUserWithEmailAndPassword(email.trim(), password).await()
                authInstance.signOut()
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

    fun updateAkun(uid: String, nama: String, role: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid).update(mapOf("nama" to nama, "role" to role)).await()
                onResult(true, "Akun $nama berhasil diperbarui")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Gagal memperbarui akun")
            }
        }
    }

    // Hanya menghapus profil Firestore; akun login Firebase Auth tetap ada (perlu Admin SDK untuk menghapusnya)
    fun hapusAkun(uid: String, nama: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid).delete().await()
                onResult(true, "Data akun $nama berhasil dihapus")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Gagal menghapus akun")
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