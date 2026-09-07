package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AiScannerResult(
    val label: String,
    val confidence: Float
)

class AiScannerViewModel : ViewModel() {

    private val labeler: ImageLabeler =
        ImageLabeling.getClient(
            ImageLabelerOptions.DEFAULT_OPTIONS
        )

    private val _result =
        MutableStateFlow<AiScannerResult?>(null)

    val result: StateFlow<AiScannerResult?> =
        _result.asStateFlow()

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()

    fun analyzeImage(bitmap: Bitmap) {

        viewModelScope.launch {

            _isLoading.value = true
            _result.value = null
            _error.value = null

            try {

                val image =
                    InputImage.fromBitmap(
                        bitmap,
                        0
                    )

                val labels =
                    labeler.process(image).await()

                if (labels.isNotEmpty()) {

                    val bestLabel =
                        labels.maxByOrNull {
                            it.confidence
                        }

                    if (bestLabel != null) {

                        _result.value =
                            AiScannerResult(
                                label = bestLabel.text,
                                confidence = bestLabel.confidence
                            )
                    }

                } else {

                    _error.value =
                        "Objek tidak dapat dikenali"
                }

            } catch (e: Exception) {

                _error.value =
                    "Gagal menganalisis gambar: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    // =========================
    // MENGUBAH HASIL AI
    // MENJADI KATEGORI SAMPAH
    // =========================
    fun getKategoriSampah(
        label: String
    ): String {

        val labelKecil =
            label.lowercase()

        return when {

            // PLASTIK
            labelKecil.contains("bottle") ||
                    labelKecil.contains("plastic") ||
                    labelKecil.contains("container") ||
                    labelKecil.contains("bag") -> {

                "Plastik"
            }

            // KERTAS
            labelKecil.contains("paper") ||
                    labelKecil.contains("cardboard") ||
                    labelKecil.contains("book") ||
                    labelKecil.contains("newspaper") -> {

                "Kertas"
            }

            // LOGAM
            labelKecil.contains("can") ||
                    labelKecil.contains("metal") ||
                    labelKecil.contains("tin") -> {

                "Logam"
            }

            // ORGANIK
            labelKecil.contains("food") ||
                    labelKecil.contains("fruit") ||
                    labelKecil.contains("vegetable") ||
                    labelKecil.contains("banana") ||
                    labelKecil.contains("apple") -> {

                "Organik"
            }

            else -> {

                "Lainnya"
            }
        }
    }

    override fun onCleared() {

        super.onCleared()

        labeler.close()
    }
}