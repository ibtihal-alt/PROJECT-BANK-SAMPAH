package com.example.sisteminformasiramahlingkungandanmanajemensampah

import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityCriteriaWeights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptivePriorityEngineTest {

    private val hour = 3_600_000L
    private val now = 100 * hour
    private val fleetLat = -7.40
    private val fleetLng = 111.44

    private fun pickup(id: Int, lat: Double, jenis: String, fill: Int, urgensi: String, waitedHours: Long) =
        PickupTransaction(
            id = id, latitude = lat, longitude = fleetLng, jenisSampah = jenis,
            tingkatKepenuhan = fill, urgensiWarga = urgensi, estimasiBeratKg = 10.0,
            tanggalJemput = now - waitedHours * hour
        )

    @Test
    fun farCriticalPointBeatsNearCalmPoint() {
        val near = pickup(1, -7.401, "Plastik", 30, "RENDAH", 1)
        val far = pickup(2, -7.45, "Organik", 95, "DARURAT", 30)
        val ranked = AdaptivePriorityEngine.rankPickups(listOf(near, far), fleetLat, fleetLng, currentTimeMs = now)
        assertEquals(2, ranked.first().pickup.id)
    }

    @Test
    fun entropyWeightDropsForCriterionWithNoSpread() {
        val expert = PriorityCriteriaWeights()
        val candidates = listOf(
            pickup(1, -7.41, "Organik", 70, "SEDANG", 5),
            pickup(2, -7.41, "Kaleng", 70, "SEDANG", 5)
        ).map { AdaptivePriorityEngine.calculateScore(it, fleetLat, fleetLng, weights = expert, currentTimeMs = now) }

        val w = AdaptivePriorityEngine.adaptiveWeights(candidates, expert)

        // Kepenuhan identik → komponen entropi 0, tersisa α·w_pakar
        assertEquals(0.5 * expert.weightFill, w.weightFill, 1e-6)
        assertTrue(w.weightWaste > expert.weightWaste)
        val sum = w.weightFill + w.weightWait + w.weightWaste + w.weightUrgency + w.weightProximity + w.weightCapacity
        assertEquals(1.0, sum, 1e-6)
    }

    @Test
    fun organicWasteAgesFasterThanInorganic() {
        val organik = AdaptivePriorityEngine.calculateScore(pickup(1, -7.41, "Organik", 50, "SEDANG", 12), fleetLat, fleetLng, currentTimeMs = now)
        val plastik = AdaptivePriorityEngine.calculateScore(pickup(2, -7.41, "Plastik", 50, "SEDANG", 12), fleetLat, fleetLng, currentTimeMs = now)
        assertTrue(organik.waitScore > plastik.waitScore)
    }

    @Test
    fun rollingDispatchRescoresFromPreviousStop() {
        val critical = pickup(1, -7.46, "Organik", 95, "DARURAT", 30)
        val nextToCritical = pickup(2, -7.461, "Plastik", 50, "SEDANG", 5)
        val nextToFleet = pickup(3, -7.401, "Plastik", 50, "SEDANG", 5)

        // Dari posisi awal armada, titik 3 lebih unggul daripada titik 2 (urutan statis)...
        val static2 = AdaptivePriorityEngine.calculateScore(nextToCritical, fleetLat, fleetLng, currentTimeMs = now)
        val static3 = AdaptivePriorityEngine.calculateScore(nextToFleet, fleetLat, fleetLng, currentTimeMs = now)
        assertTrue(static3.totalScore > static2.totalScore)

        // ...tetapi setelah armada tiba di titik kritis, titik 2 yang bersebelahan dinilai ulang lebih tinggi.
        val ranked = AdaptivePriorityEngine.rankPickups(listOf(nextToFleet, nextToCritical, critical), fleetLat, fleetLng, currentTimeMs = now)
        assertEquals(listOf(1, 2, 3), ranked.map { it.pickup.id })
    }
}
