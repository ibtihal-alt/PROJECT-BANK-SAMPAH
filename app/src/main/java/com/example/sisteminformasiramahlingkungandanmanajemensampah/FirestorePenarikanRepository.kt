package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.util.Log
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PenarikanSaldo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

// users/{uid}/penarikan/{id} adalah sumber data pencairan saldo untuk semua HP; Room hanya cache lokal
class FirestorePenarikanRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("Pengguna belum login")
    }

    private fun penarikanCollection(userId: String) =
        firestore.collection("users").document(userId).collection("penarikan")

    fun newPenarikanId(userId: String): String = penarikanCollection(userId).document().id

    suspend fun simpanPenarikan(penarikan: PenarikanSaldo) {
        // Data lama yang dibuat sebelum sinkronisasi tidak punya firebaseId; tetap lokal saja
        if (penarikan.userId.isBlank() || penarikan.firebaseId.isBlank()) return
        try {
            penarikanCollection(penarikan.userId).document(penarikan.firebaseId)
                .set(toMap(penarikan), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("FIREBASE_SIRAM", "Gagal menyimpan penarikan ke Firestore", e)
            throw e
        }
    }

    // Staf (admin & super admin) mendengar semua penarikan; nasabah hanya miliknya sendiri
    fun listenPenarikan(asStaff: Boolean, onChange: (List<DocumentSnapshot>) -> Unit): ListenerRegistration {
        val query = if (asStaff) firestore.collectionGroup("penarikan") else penarikanCollection(getUserId())
        return query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("FIREBASE_SIRAM", "Gagal sinkron penarikan", e)
            } else if (snapshot != null) {
                onChange(snapshot.documents)
            }
        }
    }

    companion object {
        fun toMap(p: PenarikanSaldo): Map<String, Any> = mapOf(
            "userId" to p.userId,
            "metodePenarikan" to p.metodePenarikan,
            "nomorTujuan" to p.nomorTujuan,
            "namaPemilik" to p.namaPemilik,
            "nominalRp" to p.nominalRp,
            "status" to p.status,
            "tanggal" to p.tanggal
        )

        fun fromDoc(doc: DocumentSnapshot, base: PenarikanSaldo): PenarikanSaldo = base.copy(
            firebaseId = doc.id,
            userId = doc.getString("userId") ?: doc.reference.parent.parent?.id ?: base.userId,
            metodePenarikan = doc.getString("metodePenarikan") ?: base.metodePenarikan,
            nomorTujuan = doc.getString("nomorTujuan") ?: base.nomorTujuan,
            namaPemilik = doc.getString("namaPemilik") ?: base.namaPemilik,
            nominalRp = doc.getDouble("nominalRp") ?: base.nominalRp,
            status = doc.getString("status") ?: base.status,
            tanggal = doc.getLong("tanggal") ?: base.tanggal
        )
    }
}
