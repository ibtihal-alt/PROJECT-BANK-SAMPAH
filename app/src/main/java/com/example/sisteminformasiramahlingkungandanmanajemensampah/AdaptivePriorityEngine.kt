package com.example.sisteminformasiramahlingkungandanmanajemensampah

import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityCriteriaWeights
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityRankedItem
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityScoreBreakdown
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityTier
import kotlin.math.*

/**
 * Adaptive Context-Aware Waste Collection Priority Engine.
 *
 * Mengimplementasikan Multi-Criteria Decision Making (MCDM) adaptif berbasis kondisi real-time:
 * 1. Tingkat Kepenuhan Tempat Sampah (Fill Level)
 * 2. Lama Waktu Menunggu (Waiting Time Aging)
 * 3. Karakteristik & Perishability Jenis Sampah (Waste Type Decomposition/Hazard)
 * 4. Jarak Armada Lapangan (Proximity via Haversine Formula)
 * 5. Kesesuaian Kapasitas Kendaraan (Fleet Remaining Payload Compatibility)
 * 6. Tingkat Urgensi Laporan Warga (Citizen Urgency/Emergency Level)
 *
 * Ditambah dengan Context-Aware Multipliers untuk pencegahan keterabaian (Anti-Starvation)
 * dan penanganan bahaya pencemaran lingkungan akut (Biohazard & Leachate Surge).
 */
object AdaptivePriorityEngine {

    /**
     * Menghitung jarak garis lurus (great-circle distance) dalam kilometer
     * antara dua koordinat GPS menggunakan formula Haversine.
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if ((lat1 == 0.0 && lon1 == 0.0) || (lat2 == 0.0 && lon2 == 0.0)) {
            return 2.5 // Jarak estimasi default jika GPS belum terkunci
        }
        val r = 6371.0 // Radius bumi dalam km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).coerceAtLeast(0.05) // Minimal 50 meter
    }

    /**
     * Menghitung skor dekomposisi / bahaya berdasarkan jenis sampah.
     * Sampah organik basah dan B3 memiliki risiko bau, lindi, dan patogen yang cepat memburuk.
     */
    fun getWasteTypeHazardScore(jenisSampah: String): Double {
        val lower = jenisSampah.lowercase().trim()
        return when {
            lower.contains("organik") || lower.contains("makanan") || lower.contains("sayur") || lower.contains("dapur") || lower.contains("basah") -> 95.0
            lower.contains("medis") || lower.contains("masker") || lower.contains("b3") || lower.contains("kimia") || lower.contains("baterai") -> 90.0
            lower.contains("campur") || lower.contains("residu") || lower.contains("daun") -> 75.0
            lower.contains("kardus") || lower.contains("kertas") -> 45.0
            lower.contains("plastik") || lower.contains("botol") -> 40.0
            lower.contains("kaleng") || lower.contains("logam") || lower.contains("besi") || lower.contains("kaca") -> 35.0
            else -> 50.0
        }
    }

    /**
     * Menghitung skor urgensi laporan warga.
     */
    fun getCitizenUrgencyScore(urgensi: String): Double {
        return when (urgensi.uppercase().trim()) {
            "DARURAT" -> 100.0 // Meluap ke jalan, banjir sampah, bau menyengat
            "TINGGI" -> 75.0   // Penumpukan signifikan, hajatan, toko ramai
            "SEDANG" -> 50.0   // Rutinitas reguler
            "RENDAH" -> 25.0   // Fleksibel, belum mendesak
            else -> 50.0
        }
    }

    /**
     * Menghitung Adaptive Waste Collection Priority (AWCP) Score secara komprehensif.
     */
    fun calculateScore(
        pickup: PickupTransaction,
        fleetLat: Double,
        fleetLng: Double,
        fleetCapacityKg: Double = 500.0,
        currentPayloadKg: Double = 0.0,
        weights: PriorityCriteriaWeights = PriorityCriteriaWeights(),
        currentTimeMs: Long = System.currentTimeMillis()
    ): PriorityScoreBreakdown {
        val explanations = mutableListOf<String>()

        // 1. Kriteria Kepenuhan Tempat Sampah (F)
        val rawFill = pickup.tingkatKepenuhan.toDouble().coerceIn(10.0, 100.0)
        val fillScore = if (rawFill >= 90.0) {
            explanations.add("Tempat sampah meluap (${rawFill.toInt()}%) melebihi ambang batas kritis.")
            100.0
        } else {
            rawFill
        }

        // 2. Kriteria Lama Waktu Menunggu (T)
        val elapsedMs = (currentTimeMs - pickup.tanggalJemput).coerceAtLeast(0L)
        val waitingHours = (elapsedMs.toDouble() / (1000.0 * 60.0 * 60.0)).coerceAtLeast(0.1)
        val waitScore = (min(100.0, (waitingHours / 24.0) * 100.0)).also {
            if (waitingHours >= 24.0) {
                explanations.add("Permintaan telah tertunda ${String.format("%.1f", waitingHours)} jam (lebih dari 1 hari).")
            }
        }

        // 3. Kriteria Jenis Sampah (W)
        val wasteScore = getWasteTypeHazardScore(pickup.jenisSampah)
        if (wasteScore >= 90.0) {
            explanations.add("Kategori '${pickup.jenisSampah}' rentan pembusukan cepat, bau busuk, dan lalat.")
        }

        // 4. Kriteria Urgensi Warga (U)
        val urgencyScore = getCitizenUrgencyScore(pickup.urgensiWarga)
        if (pickup.urgensiWarga.equals("DARURAT", ignoreCase = true)) {
            explanations.add("Warga menandai status DARURAT dengan kebutuhan evakuasi cepat.")
        }

        // 5. Kriteria Jarak Armada (D)
        val distanceKm = calculateDistanceKm(fleetLat, fleetLng, pickup.latitude, pickup.longitude)
        // Skor kedekatan armada meluruh halus: makin dekat makin tinggi
        val proximityScore = (100.0 / (1.0 + 0.35 * distanceKm)).coerceIn(10.0, 100.0)

        // 6. Kriteria Kesesuaian Kapasitas Kendaraan (C)
        val sisaKapasitasKg = (fleetCapacityKg - currentPayloadKg).coerceAtLeast(0.0)
        val beratMuatan = if (pickup.estimasiBeratKg > 0) pickup.estimasiBeratKg else 5.0
        val capacityScore = if (beratMuatan <= sisaKapasitasKg) {
            // Armada muat penuh
            val rasioMuatan = (beratMuatan / sisaKapasitasKg.coerceAtLeast(1.0)).coerceIn(0.0, 1.0)
            80.0 + (20.0 * rasioMuatan)
        } else {
            // Armada kekurangan kapasitas (akan overload jika dipaksakan sekaligus)
            explanations.add("Muatan (${beratMuatan.toInt()} kg) mendekati/melebihi sisa kapasitas armada (${sisaKapasitasKg.toInt()} kg).")
            max(20.0, 60.0 - ((beratMuatan - sisaKapasitasKg) / fleetCapacityKg) * 80.0)
        }

        // Hitung Base Weighted Score (MCDM)
        val baseScore = (fillScore * weights.weightFill) +
                (waitScore * weights.weightWait) +
                (wasteScore * weights.weightWaste) +
                (urgencyScore * weights.weightUrgency) +
                (proximityScore * weights.weightProximity) +
                (capacityScore * weights.weightCapacity)

        // Context-Aware Boosters (Aturan Adaptif Situasional)
        var contextBoost = 0.0

        // Booster 1: Kondisi Darurat Meluap & Organik Membusuk
        if (rawFill >= 85.0 && (wasteScore >= 90.0 || urgencyScore >= 90.0)) {
            contextBoost += 12.0
            explanations.add("Boost Kondisi Kritis (+12.0): Pencegahan pencemaran lingkungan & keluhan publik.")
        }

        // Booster 2: Anti-Starvation (Mencegah titik lama terlantar karena terus dikalahkan titik dekat baru)
        if (waitingHours >= 36.0) {
            contextBoost += 10.0
            explanations.add("Boost Anti-Starvation (+10.0): Penjemputan tertunda lama diprioritaskan naik.")
        }

        val totalScore = (baseScore + contextBoost).coerceIn(0.0, 100.0)

        val tier = when {
            totalScore >= 80.0 -> PriorityTier.CRITICAL
            totalScore >= 65.0 -> PriorityTier.HIGH
            totalScore >= 45.0 -> PriorityTier.MEDIUM
            else -> PriorityTier.NORMAL
        }

        // Susun ringkasan penalaran kecerdasan sistem
        val reasoningSummary = when {
            tier == PriorityTier.CRITICAL -> {
                "Sangat Mendesak! ${if (rawFill >= 90) "Tempat sampah meluap (${rawFill.toInt()}%)" else "Status Darurat"} dan ${if (wasteScore >= 90) "sampah organik membusuk" else "menunggu ${String.format("%.0f", waitingHours)} jam"}. Didahulukan dari titik lain."
            }
            tier == PriorityTier.HIGH -> {
                "Prioritas Tinggi. Volume sampah signifikan (${rawFill.toInt()}%) dengan waktu tunggu ${String.format("%.1f", waitingHours)} jam, efisien diambil dalam siklus ini."
            }
            tier == PriorityTier.MEDIUM -> {
                "Prioritas Menengah. Jadwal penjemputan teratur dengan jarak ${String.format("%.1f", distanceKm)} km dari posisi armada."
            }
            else -> {
                "Reguler. Kondisi tempat sampah aman (${rawFill.toInt()}%), jenis anorganik stabil, belum mendesak."
            }
        }

        return PriorityScoreBreakdown(
            totalScore = totalScore,
            tier = tier,
            fillScore = fillScore,
            waitScore = waitScore,
            wasteScore = wasteScore,
            proximityScore = proximityScore,
            capacityScore = capacityScore,
            urgencyScore = urgencyScore,
            distanceKm = distanceKm,
            waitingHours = waitingHours,
            contextBoostApplied = contextBoost,
            reasoningSummary = reasoningSummary,
            explanationDetails = explanations
        )
    }

    /**
     * Memproses dan mengurutkan daftar penjemputan berdasarkan AWCP Score (Ranking Tertinggi ke Terendah).
     */
    fun rankPickups(
        pickups: List<PickupTransaction>,
        fleetLat: Double,
        fleetLng: Double,
        fleetCapacityKg: Double = 500.0,
        currentPayloadKg: Double = 0.0,
        weights: PriorityCriteriaWeights = PriorityCriteriaWeights(),
        currentTimeMs: Long = System.currentTimeMillis()
    ): List<PriorityRankedItem> {
        return pickups.map { pickup ->
            val breakdown = calculateScore(
                pickup = pickup,
                fleetLat = fleetLat,
                fleetLng = fleetLng,
                fleetCapacityKg = fleetCapacityKg,
                currentPayloadKg = currentPayloadKg,
                weights = weights,
                currentTimeMs = currentTimeMs
            )
            pickup to breakdown
        }
            .sortedByDescending { it.second.totalScore }
            .mapIndexed { index, (pickup, breakdown) ->
                PriorityRankedItem(
                    pickup = pickup,
                    breakdown = breakdown,
                    rank = index + 1
                )
            }
    }

    /**
     * Menghasilkan narasi perbandingan cerdas antara dua titik penjemputan:
     * Menjelaskan MENGAPA Titik A didahulukan dari Titik B meskipun Titik B mungkin lebih dekat lokasinya.
     */
    fun explainTradeoff(
        higherItem: PriorityRankedItem,
        lowerItem: PriorityRankedItem
    ): String {
        val a = higherItem
        val b = lowerItem

        val distA = a.breakdown.distanceKm
        val distB = b.breakdown.distanceKm

        return if (distA > distB) {
            val selisihJarak = distA - distB
            "Meskipun titik #${a.rank} (${a.pickup.jenisSampah}) berjarak ${String.format("%.1f", selisihJarak)} km LEBIH JAUH daripada titik #${b.rank}, sistem memprioritaskannya karena: Kepenuhan ${a.pickup.tingkatKepenuhan}% vs ${b.pickup.tingkatKepenuhan}%, Urgensi ${a.pickup.urgensiWarga}, dan Waktu tunggu ${String.format("%.0f", a.breakdown.waitingHours)} jam."
        } else {
            "Titik #${a.rank} didahulukan karena memiliki kombinasi kepenuhan (${a.pickup.tingkatKepenuhan}%) dan kedekatan posisi (${String.format("%.1f", distA)} km) yang paling optimal."
        }
    }
}
