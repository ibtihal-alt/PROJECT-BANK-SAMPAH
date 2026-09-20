package com.example.sisteminformasiramahlingkungandanmanajemensampah

import com.google.firebase.firestore.FirebaseFirestore

object NotificationHelper {
    fun kirimNotifikasi(userId: String, title: String, message: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val notifData = hashMapOf(
                "userId" to userId,
                "title" to title,
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false
            )
            firestore.collection("notifications").add(notifData)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun formatPhoneNumberForWhatsApp(phone: String): String {
        val digitsOnly = phone.filter { it.isDigit() }
        return when {
            digitsOnly.startsWith("0") -> "62" + digitsOnly.substring(1)
            digitsOnly.startsWith("8") -> "62" + digitsOnly
            digitsOnly.startsWith("62") -> digitsOnly
            else -> digitsOnly
        }
    }
}
