package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("siram_theme_prefs", Context.MODE_PRIVATE)

    var isDarkMode by mutableStateOf(prefs.getBoolean("is_dark_mode", false))
        private set

    // Menggunakan nama toggleTheme agar tidak bentrok dengan setter otomatis dari mutableStateOf
    fun toggleTheme(enabled: Boolean) {
        isDarkMode = enabled
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }
}