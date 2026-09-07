package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.AppDatabase

class SampahViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(SampahViewModel::class.java)) {

            val database = AppDatabase.getDatabase(context)

            return SampahViewModel(
                database.sampahDao()
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}