package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.util.Log
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

// users/{uid}/pickup_requests/{id} adalah sumber data penjemputan untuk semua HP; Room hanya cache lokal
class FirestorePickupRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("Pengguna belum login")
    }

    private fun pickupCollection(userId: String) =
        firestore.collection("users").document(userId).collection("pickup_requests")

    fun newPickupId(userId: String): String = pickupCollection(userId).document().id

    suspend fun simpanPickup(pickup: PickupTransaction) {
        // Data lama yang dibuat sebelum sinkronisasi tidak punya firebaseId; tetap lokal saja
        if (pickup.userId.isBlank() || pickup.firebaseId.isBlank()) return
        try {
            pickupCollection(pickup.userId).document(pickup.firebaseId)
                .set(toMap(pickup), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("FIREBASE_SIRAM", "Gagal menyimpan penjemputan ke Firestore", e)
            throw e
        }
    }

    // Staf (admin & super admin) mendengar semua penjemputan; nasabah hanya miliknya sendiri
    fun listenPickups(asStaff: Boolean, onChange: (List<DocumentSnapshot>) -> Unit): ListenerRegistration {
        val query = if (asStaff) firestore.collectionGroup("pickup_requests") else pickupCollection(getUserId())
        return query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("FIREBASE_SIRAM", "Gagal sinkron penjemputan", e)
            } else if (snapshot != null) {
                onChange(snapshot.documents)
            }
        }
    }

    // Foto disimpan sebagai Blob JPEG di pickup_photos/{pickupId}_{tahap} (tanpa Firebase Storage); batas dokumen 1 MB
    suspend fun uploadFoto(firebaseId: String, tahap: String, jpeg: ByteArray) {
        require(jpeg.size < 1_000_000) { "Ukuran foto terlalu besar" }
        fotoDoc(firebaseId, tahap).set(
            mapOf(
                "pickupId" to firebaseId,
                "tahap" to tahap,
                "data" to Blob.fromBytes(jpeg),
                "uploadedBy" to getUserId(),
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    fun listenFoto(firebaseId: String, tahap: String, onChange: (ByteArray?) -> Unit): ListenerRegistration =
        fotoDoc(firebaseId, tahap).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("FIREBASE_SIRAM", "Gagal memuat foto $tahap", e)
            } else {
                onChange(snapshot?.getBlob("data")?.toBytes())
            }
        }

    private fun fotoDoc(firebaseId: String, tahap: String) =
        firestore.collection("pickup_photos").document("${firebaseId}_$tahap")

    companion object {
        fun toMap(p: PickupTransaction): Map<String, Any> = mapOf(
            "userId" to p.userId,
            "jenisSampah" to p.jenisSampah,
            "estimasiBeratKg" to p.estimasiBeratKg,
            "beratAktualKg" to p.beratAktualKg,
            "hargaPerKgAktual" to p.hargaPerKgAktual,
            "totalNominalRp" to p.totalNominalRp,
            "alamatLengkap" to p.alamatLengkap,
            "nomorHp" to p.nomorHp,
            "tanggalJemput" to p.tanggalJemput,
            "createdAt" to p.tanggalJemput,
            "catatan" to p.catatan,
            "status" to p.status,
            "assignedKurirId" to p.assignedKurirId,
            "assignedKurirName" to p.assignedKurirName,
            "fotoBuktiUri" to p.fotoBuktiUri,
            "latitude" to p.latitude,
            "longitude" to p.longitude,
            "tingkatKepenuhan" to p.tingkatKepenuhan,
            "urgensiWarga" to p.urgensiWarga,
            "kapasitasTempatSampahLiter" to p.kapasitasTempatSampahLiter,
            "jenisWadah" to p.jenisWadah
        )

        // Field yang belum ada di dokumen lama memakai nilai dari base (data lokal) agar tidak tertimpa kosong
        fun fromDoc(doc: DocumentSnapshot, base: PickupTransaction): PickupTransaction = base.copy(
            firebaseId = doc.id,
            userId = doc.reference.parent.parent?.id ?: base.userId,
            jenisSampah = doc.getString("jenisSampah") ?: base.jenisSampah,
            estimasiBeratKg = doc.getDouble("estimasiBeratKg") ?: base.estimasiBeratKg,
            beratAktualKg = doc.getDouble("beratAktualKg") ?: base.beratAktualKg,
            hargaPerKgAktual = doc.getDouble("hargaPerKgAktual") ?: base.hargaPerKgAktual,
            totalNominalRp = doc.getDouble("totalNominalRp") ?: base.totalNominalRp,
            alamatLengkap = doc.getString("alamatLengkap") ?: base.alamatLengkap,
            nomorHp = doc.getString("nomorHp") ?: base.nomorHp,
            tanggalJemput = doc.getLong("tanggalJemput") ?: base.tanggalJemput,
            catatan = doc.getString("catatan") ?: base.catatan,
            status = doc.getString("status") ?: base.status,
            assignedKurirId = doc.getString("assignedKurirId") ?: base.assignedKurirId,
            assignedKurirName = doc.getString("assignedKurirName") ?: base.assignedKurirName,
            fotoBuktiUri = doc.getString("fotoBuktiUri") ?: base.fotoBuktiUri,
            latitude = doc.getDouble("latitude") ?: base.latitude,
            longitude = doc.getDouble("longitude") ?: base.longitude,
            tingkatKepenuhan = doc.getLong("tingkatKepenuhan")?.toInt() ?: base.tingkatKepenuhan,
            urgensiWarga = doc.getString("urgensiWarga") ?: base.urgensiWarga,
            kapasitasTempatSampahLiter = doc.getDouble("kapasitasTempatSampahLiter") ?: base.kapasitasTempatSampahLiter,
            jenisWadah = doc.getString("jenisWadah") ?: base.jenisWadah
        )
    }
}
