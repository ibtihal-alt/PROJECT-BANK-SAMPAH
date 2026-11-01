package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.util.Log
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

// users/{uid}/sampah/{id} adalah sumber data setor sampah & saldo untuk semua HP; Room hanya cache lokal
class FirestoreSampahRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("Pengguna belum login")
    }

    // Data disimpan di bawah pemilik sampah (nasabah), bukan akun yang sedang login (bisa admin)
    private fun sampahCollection(userId: String) =
        firestore.collection("users").document(userId).collection("sampah")

    fun newSampahId(userId: String): String = sampahCollection(userId).document().id

    suspend fun simpanSampah(sampah: Sampah) {
        // Data lama yang dibuat sebelum sinkronisasi tidak punya firebaseId; tetap lokal saja
        if (sampah.userId.isBlank() || sampah.firebaseId.isBlank()) return
        try {
            sampahCollection(sampah.userId).document(sampah.firebaseId)
                .set(toMap(sampah), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("FIREBASE_SIRAM", "Gagal menyimpan sampah ke Firestore", e)
            throw e
        }
    }

    // Staf (admin & super admin) mendengar semua setoran; nasabah hanya miliknya sendiri
    fun listenSampah(asStaff: Boolean, onChange: (List<DocumentSnapshot>) -> Unit): ListenerRegistration {
        val query = if (asStaff) firestore.collectionGroup("sampah") else sampahCollection(getUserId())
        return query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("FIREBASE_SIRAM", "Gagal sinkron sampah", e)
            } else if (snapshot != null) {
                onChange(snapshot.documents)
            }
        }
    }

    companion object {
        fun toMap(s: Sampah): Map<String, Any> = mapOf(
            "userId" to s.userId,
            "jenisSampah" to s.jenisSampah,
            "berat" to s.berat,
            "hargaPerKg" to s.hargaPerKg,
            "totalNilai" to s.totalNilai,
            "catatan" to s.catatan,
            "tanggal" to s.tanggal,
            "status" to s.status,
            "isDeleted" to s.isDeleted
        )

        // Field yang belum ada di dokumen lama memakai nilai dari base (data lokal) agar tidak tertimpa kosong
        fun fromDoc(doc: DocumentSnapshot, base: Sampah): Sampah = base.copy(
            firebaseId = doc.id,
            userId = doc.getString("userId") ?: base.userId.ifBlank { doc.reference.parent.parent?.id ?: "" },
            jenisSampah = doc.getString("jenisSampah") ?: base.jenisSampah,
            berat = doc.getDouble("berat") ?: base.berat,
            hargaPerKg = doc.getDouble("hargaPerKg") ?: base.hargaPerKg,
            totalNilai = doc.getDouble("totalNilai") ?: base.totalNilai,
            catatan = doc.getString("catatan") ?: base.catatan,
            tanggal = doc.getLong("tanggal") ?: base.tanggal,
            status = doc.getString("status") ?: base.status,
            isDeleted = doc.getBoolean("isDeleted") ?: base.isDeleted
        )
    }
}
