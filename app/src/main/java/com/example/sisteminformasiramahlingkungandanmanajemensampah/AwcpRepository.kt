package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.util.Log
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityCriteriaWeights
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityRankedItem
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityScoreBreakdown
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityTier
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.SpkConfig
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Data SPK bersama antar HP:
 * - spk_config/global          : bobot kriteria + kapasitas armada (diubah Super Admin)
 * - awcp_scores/{pickupFirebaseId} : hasil AWCP Score per penjemputan (dibaca Super Admin & kurir)
 */
class AwcpRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val configRef = firestore.collection("spk_config").document("global")
    private val scores = firestore.collection("awcp_scores")

    fun listenConfig(onChange: (SpkConfig) -> Unit): ListenerRegistration =
        configRef.addSnapshotListener { doc, e ->
            if (e != null) { Log.e("SIRAM_SPK", "Gagal membaca pengaturan SPK", e); return@addSnapshotListener }
            onChange(if (doc != null && doc.exists()) configFromDoc(doc) else SpkConfig())
        }

    suspend fun simpanConfig(config: SpkConfig) {
        val w = config.weights
        configRef.set(
            mapOf(
                "weightFill" to w.weightFill,
                "weightWait" to w.weightWait,
                "weightWaste" to w.weightWaste,
                "weightUrgency" to w.weightUrgency,
                "weightProximity" to w.weightProximity,
                "weightCapacity" to w.weightCapacity,
                "fleetCapacityKg" to config.fleetCapacityKg,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    /** kurirId null = semua skor (Super Admin); selain itu hanya kelompok kurir tersebut. */
    fun listenScores(kurirId: String?, onChange: (Map<String, DocumentSnapshot>) -> Unit): ListenerRegistration {
        val query = if (kurirId == null) scores else scores.whereEqualTo("kurirId", kurirId)
        return query.addSnapshotListener { snap, e ->
            if (e != null) { Log.e("SIRAM_SPK", "Gagal membaca skor AWCP", e); return@addSnapshotListener }
            onChange(snap?.documents.orEmpty().associateBy { it.id })
        }
    }

    /** Posisi GPS terakhir tiap kurir dari koleksi fleets (id dokumen = uid kurir). */
    suspend fun ambilArmada(): List<Map<String, Any>> =
        firestore.collection("fleets").get().await().documents.map { doc ->
            (doc.data.orEmpty() + ("courierId" to doc.id))
        }

    suspend fun simpanScores(items: List<PriorityRankedItem>, hapusIds: Collection<String>) {
        val ops = items.filter { it.pickup.firebaseId.isNotBlank() }.map { it.pickup.firebaseId to toMap(it) } +
                hapusIds.map { it to null }
        // Batas Firestore 500 operasi per batch
        ops.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { (id, data) ->
                if (data == null) batch.delete(scores.document(id)) else batch.set(scores.document(id), data)
            }
            batch.commit().await()
        }
    }

    companion object {
        fun configFromDoc(doc: DocumentSnapshot): SpkConfig {
            val d = PriorityCriteriaWeights()
            return SpkConfig(
                weights = PriorityCriteriaWeights(
                    weightFill = doc.getDouble("weightFill") ?: d.weightFill,
                    weightWait = doc.getDouble("weightWait") ?: d.weightWait,
                    weightWaste = doc.getDouble("weightWaste") ?: d.weightWaste,
                    weightUrgency = doc.getDouble("weightUrgency") ?: d.weightUrgency,
                    weightProximity = doc.getDouble("weightProximity") ?: d.weightProximity,
                    weightCapacity = doc.getDouble("weightCapacity") ?: d.weightCapacity
                ),
                fleetCapacityKg = doc.getDouble("fleetCapacityKg") ?: 500.0
            )
        }

        fun toMap(item: PriorityRankedItem): Map<String, Any> {
            val b = item.breakdown
            val w = b.appliedWeights
            return mapOf(
                "pickupId" to item.pickup.firebaseId,
                "kurirId" to item.kurirId,
                "rank" to item.rank,
                "totalScore" to b.totalScore,
                "tier" to b.tier.name,
                "fillScore" to b.fillScore,
                "waitScore" to b.waitScore,
                "wasteScore" to b.wasteScore,
                "proximityScore" to b.proximityScore,
                "capacityScore" to b.capacityScore,
                "urgencyScore" to b.urgencyScore,
                "distanceKm" to b.distanceKm,
                "waitingHours" to b.waitingHours,
                "contextBoost" to b.contextBoostApplied,
                "reasoningSummary" to b.reasoningSummary,
                "explanationDetails" to b.explanationDetails,
                "appliedWeights" to mapOf(
                    "fill" to w.weightFill, "wait" to w.weightWait, "waste" to w.weightWaste,
                    "urgency" to w.weightUrgency, "proximity" to w.weightProximity, "capacity" to w.weightCapacity
                ),
                "originLat" to item.originLat,
                "originLng" to item.originLng,
                "computedAt" to item.computedAt
            )
        }

        fun fromDoc(doc: DocumentSnapshot, pickup: PickupTransaction): PriorityRankedItem {
            @Suppress("UNCHECKED_CAST")
            val w = doc.get("appliedWeights") as? Map<String, Number> ?: emptyMap()
            val d = PriorityCriteriaWeights()
            @Suppress("UNCHECKED_CAST")
            val explanations = doc.get("explanationDetails") as? List<String> ?: emptyList()
            return PriorityRankedItem(
                pickup = pickup,
                rank = doc.getLong("rank")?.toInt() ?: 0,
                kurirId = doc.getString("kurirId") ?: "",
                originLat = doc.getDouble("originLat") ?: 0.0,
                originLng = doc.getDouble("originLng") ?: 0.0,
                computedAt = doc.getLong("computedAt") ?: 0L,
                breakdown = PriorityScoreBreakdown(
                    totalScore = doc.getDouble("totalScore") ?: 0.0,
                    tier = runCatching { PriorityTier.valueOf(doc.getString("tier") ?: "") }.getOrDefault(PriorityTier.NORMAL),
                    fillScore = doc.getDouble("fillScore") ?: 0.0,
                    waitScore = doc.getDouble("waitScore") ?: 0.0,
                    wasteScore = doc.getDouble("wasteScore") ?: 0.0,
                    proximityScore = doc.getDouble("proximityScore") ?: 0.0,
                    capacityScore = doc.getDouble("capacityScore") ?: 0.0,
                    urgencyScore = doc.getDouble("urgencyScore") ?: 0.0,
                    distanceKm = doc.getDouble("distanceKm") ?: 0.0,
                    waitingHours = doc.getDouble("waitingHours") ?: 0.0,
                    contextBoostApplied = doc.getDouble("contextBoost") ?: 0.0,
                    reasoningSummary = doc.getString("reasoningSummary") ?: "",
                    explanationDetails = explanations,
                    appliedWeights = PriorityCriteriaWeights(
                        weightFill = w["fill"]?.toDouble() ?: d.weightFill,
                        weightWait = w["wait"]?.toDouble() ?: d.weightWait,
                        weightWaste = w["waste"]?.toDouble() ?: d.weightWaste,
                        weightUrgency = w["urgency"]?.toDouble() ?: d.weightUrgency,
                        weightProximity = w["proximity"]?.toDouble() ?: d.weightProximity,
                        weightCapacity = w["capacity"]?.toDouble() ?: d.weightCapacity
                    )
                )
            )
        }
    }
}
