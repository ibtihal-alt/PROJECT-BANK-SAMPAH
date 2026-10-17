package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

import androidx.compose.ui.graphics.Color

/**
 * Tingkat Prioritas Penjemputan berdasarkan Adaptive Waste Collection Priority (AWCP) Score.
 */
enum class PriorityTier(
    val label: String,
    val badgeColorHex: Long,
    val containerColorHex: Long,
    val deskripsi: String
) {
    CRITICAL(
        label = "DARURAT / KRITIS",
        badgeColorHex = 0xFFDC2626, // Red 600
        containerColorHex = 0xFFFEE2E2, // Red 100
        deskripsi = "Kondisi sangat mendesak (meluap/berbau/menunggu lama). Wajib segera dijemput!"
    ),
    HIGH(
        label = "PRIORITAS TINGGI",
        badgeColorHex = 0xFFEA580C, // Orange 600
        containerColorHex = 0xFFFFEDD5, // Orange 100
        deskripsi = "Kapasitas tempat sampah hampir penuh atau sampah mudah membusuk."
    ),
    MEDIUM(
        label = "PRIORITAS SEDANG",
        badgeColorHex = 0xFFCA8A04, // Yellow 600
        containerColorHex = 0xFFFEF9C3, // Yellow 100
        deskripsi = "Penjemputan terjadwal dalam antrean rute normal armada."
    ),
    NORMAL(
        label = "REGULER / NORMAL",
        badgeColorHex = 0xFF16A34A, // Green 600
        containerColorHex = 0xFFDCFCE7, // Green 100
        deskripsi = "Sampah anorganik stabil, volume rendah atau belum mendesak."
    )
}

/**
 * Bobot Multi-Kriteria untuk perhitungan AWCP Score (Total = 1.0).
 * Dapat disesuaikan secara adaptif oleh Super Admin / Dispatcher.
 */
data class PriorityCriteriaWeights(
    val weightFill: Double = 0.25,      // Tingkat kepenuhan tempat sampah (25%)
    val weightWait: Double = 0.20,      // Lama waktu menunggu (20%)
    val weightWaste: Double = 0.15,     // Karakteristik jenis sampah (15%)
    val weightUrgency: Double = 0.20,   // Urgensi permintaan warga (20%)
    val weightProximity: Double = 0.10, // Jarak armada (10%)
    val weightCapacity: Double = 0.10   // Kecocokan kapasitas kendaraan armada (10%)
)

/**
 * Rincian transparansi kalkulasi skor AWCP untuk Sistem Pendukung Keputusan (SPK).
 */
data class PriorityScoreBreakdown(
    val totalScore: Double,                 // 0.0 - 100.0
    val tier: PriorityTier,
    val fillScore: Double,                 // 0.0 - 100.0
    val waitScore: Double,                 // 0.0 - 100.0
    val wasteScore: Double,                // 0.0 - 100.0
    val proximityScore: Double,            // 0.0 - 100.0
    val capacityScore: Double,             // 0.0 - 100.0
    val urgencyScore: Double,              // 0.0 - 100.0
    val distanceKm: Double,                // Jarak real-time dari armada (km)
    val waitingHours: Double,              // Lama menunggu dalam jam
    val contextBoostApplied: Double,       // Skor tambahan kondisi darurat lingkungan
    val reasoningSummary: String,          // Penjelasan ilmiah kenapa titik ini didahulukan
    val explanationDetails: List<String>   // Poin-poin faktor penentu
)

/**
 * Item penjemputan terurut dengan breakdown hasil DSS.
 */
data class PriorityRankedItem(
    val pickup: PickupTransaction,
    val breakdown: PriorityScoreBreakdown,
    val rank: Int
)
