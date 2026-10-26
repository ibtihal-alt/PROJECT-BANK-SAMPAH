package com.example.sisteminformasiramahlingkungandanmanajemensampah

import org.junit.Assert.assertEquals
import org.junit.Test

class CourierFilterTest {
    private val now = 10_000_000L
    private val budi = mapOf<String, Any>("namaKurir" to "Budi Santoso", "timestamp" to now - 60_000L)
    private val sari = mapOf<String, Any>("namaKurir" to "Sari", "timestamp" to now - COURIER_ONLINE_WINDOW_MS - 1)
    private val tanpaWaktu = mapOf<String, Any>("namaKurir" to "Andi")
    private val all = listOf(budi, sari, tanpaWaktu)

    @Test
    fun filterByOnlineStatus() {
        assertEquals(all, filterCouriers(all, "", null, now))
        assertEquals(listOf(budi), filterCouriers(all, "", true, now))
        assertEquals(listOf(sari, tanpaWaktu), filterCouriers(all, "", false, now))
    }

    @Test
    fun searchIsCaseInsensitiveAndCombinesWithFilter() {
        assertEquals(listOf(budi), filterCouriers(all, "  budi ", null, now))
        assertEquals(emptyList<Map<String, Any>>(), filterCouriers(all, "budi", false, now))
    }
}
