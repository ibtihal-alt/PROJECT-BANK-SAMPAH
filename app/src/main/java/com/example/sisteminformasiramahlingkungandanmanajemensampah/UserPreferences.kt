package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream

class UserPreferences(val context: Context) {
    private val prefs = context.getSharedPreferences("siram_user_prefs", Context.MODE_PRIVATE)

    var nomorHp by mutableStateOf(prefs.getString("nomor_hp", "") ?: "")
        private set

    var alamatDefault by mutableStateOf(prefs.getString("alamat_default", "") ?: "")
        private set

    var fotoProfil by mutableStateOf(prefs.getString("foto_profil", "") ?: "")
        private set

    fun saveUserHp(hp: String) {
        nomorHp = hp
        prefs.edit().putString("nomor_hp", hp).apply()
    }

    fun saveAlamatDefault(alamat: String) {
        alamatDefault = alamat
        prefs.edit().putString("alamat_default", alamat).apply()
    }

    fun saveFotoProfil(foto: String) {
        fotoProfil = foto
        prefs.edit().putString("foto_profil", foto).apply()
    }

    fun saveFotoProfilLokal(uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "user_profile_avatar_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            val localPath = file.absolutePath
            saveFotoProfil(localPath)
            localPath
        } catch (e: Exception) {
            ""
        }
    }
}
