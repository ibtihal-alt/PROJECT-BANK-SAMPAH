package com.example.sisteminformasiramahlingkungandanmanajemensampah.data

data class UserRole(
    val uid: String = "",
    val email: String = "",
    val nama: String = "",
    val role: String = "nasabah" // "nasabah" atau "admin"
)