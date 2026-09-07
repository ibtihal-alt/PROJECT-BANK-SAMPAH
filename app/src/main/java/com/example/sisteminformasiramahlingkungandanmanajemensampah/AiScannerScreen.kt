package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AiScannerScreen(
    onBackClick: () -> Unit = {},
    onGunakanHasil: (String) -> Unit = {},
    viewModel: AiScannerViewModel = viewModel()
) {

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    val result by viewModel.result.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    val error by viewModel.error.collectAsState()

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { photo ->

            if (photo != null) {

                bitmap = photo

                viewModel.analyzeImage(photo)
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 20.dp,
                vertical = 24.dp
            )
    ) {

        // =====================================
        // HEADER
        // =====================================

        Text(
            text = "AI Scanner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text =
                "Ambil foto sampah untuk membantu mengenali jenis dan kategorinya.",
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // =====================================
        // AREA FOTO
        // =====================================

        if (bitmap == null) {

            SiramCard(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 24.dp,
                            vertical = 40.dp
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "🤖",
                        fontSize = 56.sp
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = "Belum ada foto",
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Gunakan kamera untuk mengambil foto sampah yang ingin dianalisis.",
                        textAlign =
                            TextAlign.Center,
                        style =
                            MaterialTheme.typography
                                .bodyMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

        } else {

            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Foto sampah yang dianalisis",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(
                        MaterialTheme.shapes.large
                    ),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =====================================
        // LOADING
        // =====================================

        if (isLoading) {

            SiramCard(
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    CircularProgressIndicator()

                    Spacer(
                        modifier =
                            Modifier.width(16.dp)
                    )

                    Column {

                        Text(
                            text = "Sedang menganalisis",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "AI sedang mengenali objek pada foto...",
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }
        }

        // =====================================
        // HASIL ANALISIS
        // =====================================

        if (result != null && !isLoading) {

            val kategori =
                viewModel.getKategoriSampah(
                    result!!.label
                )

            val persentase =
                (result!!.confidence * 100)
                    .toInt()

            SiramCard(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "HASIL ANALISIS",
                        style =
                            MaterialTheme.typography
                                .labelMedium,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme
                                .primary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text =
                            result!!.label,
                        style =
                            MaterialTheme.typography
                                .headlineSmall,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    // KATEGORI

                    Text(
                        text = "Kategori Sampah",
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        text = kategori,
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme
                                .primary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )

                    // TINGKAT KEPERCAYAAN

                    Text(
                        text = "Tingkat Kepercayaan",
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        text = "$persentase%",
                        style =
                            MaterialTheme.typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Button(
                        onClick = {
                            onGunakanHasil(kategori)
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                "Gunakan Hasil"
                        )
                    }
                }
            }
        }

        // =====================================
        // ERROR
        // =====================================

        if (error != null) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            SiramCard(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column {

                    Text(
                        text =
                            "Analisis gagal",
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme
                                .error
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = error!!,
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }

        // Mengisi ruang kosong agar tombol berada di bawah
        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        // =====================================
        // TOMBOL AMBIL FOTO
        // =====================================

        Button(
            onClick = {

                cameraLauncher.launch(null)
            },
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !isLoading
        ) {

            Text(
                text =
                    if (bitmap == null) {
                        "Ambil Foto"
                    } else {
                        "Ambil Foto Baru"
                    }
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        // =====================================
        // TOMBOL KEMBALI
        // =====================================

        OutlinedButton(
            onClick = onBackClick,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Kembali"
            )
        }
    }
}