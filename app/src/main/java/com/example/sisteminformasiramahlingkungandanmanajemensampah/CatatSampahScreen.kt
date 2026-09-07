package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CatatSampahScreen(
    viewModel: SampahViewModel,
    jenisAwal: String = "",
    onSimpanClick: () -> Unit = {}
) {

    var jenisSampah by remember(jenisAwal) {
        mutableStateOf(jenisAwal)
    }

    var berat by remember {
        mutableStateOf("")
    }

    var catatan by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                horizontal = 20.dp,
                vertical = 24.dp
            )
    ) {

        // =========================
        // HEADER
        // =========================

        Text(
            text = "Catat Sampah",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Tambahkan data sampah yang telah Anda kelola.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =========================
        // FORM CARD
        // =========================

        SiramCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Informasi Sampah",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =========================
            // JENIS SAMPAH
            // =========================

            SiramTextField(
                value = jenisSampah,
                onValueChange = {
                    jenisSampah = it
                    errorMessage = ""
                },
                label = "Jenis Sampah",
                placeholder = "Contoh: Plastik"
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // =========================
            // BERAT
            // =========================

            SiramTextField(
                value = berat,
                onValueChange = {
                    berat = it
                    errorMessage = ""
                },
                label = "Berat Sampah",
                placeholder = "Contoh: 2.5",
                keyboardType = KeyboardType.Decimal
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // =========================
            // CATATAN
            // =========================

            SiramTextField(
                value = catatan,
                onValueChange = {
                    catatan = it
                },
                label = "Catatan",
                placeholder = "Tambahkan keterangan jika diperlukan"
            )

            if (errorMessage.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // =========================
            // SIMPAN
            // =========================

            Button(
                onClick = {

                    when {

                        jenisSampah.isBlank() -> {

                            errorMessage =
                                "Jenis sampah wajib diisi"
                        }

                        berat.isBlank() -> {

                            errorMessage =
                                "Berat sampah wajib diisi"
                        }

                        berat.toDoubleOrNull() == null -> {

                            errorMessage =
                                "Berat harus berupa angka"
                        }

                        berat.toDouble() <= 0 -> {

                            errorMessage =
                                "Berat harus lebih dari 0 Kg"
                        }

                        else -> {

                            viewModel.simpanSampah(
                                jenisSampah =
                                    jenisSampah.trim(),

                                berat =
                                    berat.toDouble(),

                                catatan =
                                    catatan.trim()
                            )

                            onSimpanClick()
                        }
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),

                shape = MaterialTheme.shapes.medium,

                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary
                )
            ) {

                Text(
                    text = "💾 SIMPAN DATA",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =========================
        // INFO CARD
        // =========================

        SiramCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = "💡 Tips",
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.primary
                )

                Text(
                    text =
                        "Pastikan jenis dan berat sampah sudah benar sebelum menyimpan data.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}