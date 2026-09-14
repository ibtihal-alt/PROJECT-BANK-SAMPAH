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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EditSampahScreen(
    sampah: Sampah,
    onSimpanClick: (Sampah) -> Unit = {}
) {
    var jenisSampah by remember { mutableStateOf(sampah.jenisSampah) }
    var beratText by remember { mutableStateOf(sampah.berat.toString()) }
    var hargaText by remember { mutableStateOf(sampah.hargaPerKg.toInt().toString()) }
    var catatan by remember { mutableStateOf(sampah.catatan) }
    var errorMessage by remember { mutableStateOf("") }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    // Hitung total estimasi saldo otomatis (Berat x Harga)
    val beratDouble = beratText.toDoubleOrNull() ?: 0.0
    val hargaDouble = hargaText.toDoubleOrNull() ?: 0.0
    val totalEstimasi = beratDouble * hargaDouble

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Edit Data Sampah",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ubah rincian berat atau harga sampah kamu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        SiramCard(modifier = Modifier.fillMaxWidth()) {
            // Jenis Sampah
            SiramTextField(
                value = jenisSampah,
                onValueChange = {
                    jenisSampah = it
                    errorMessage = ""
                },
                label = "Jenis Sampah"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Berat Sampah
            SiramTextField(
                value = beratText,
                onValueChange = {
                    beratText = it
                    errorMessage = ""
                },
                label = "Berat (Kg)",
                keyboardType = KeyboardType.Decimal
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Harga per Kg
            SiramTextField(
                value = hargaText,
                onValueChange = {
                    hargaText = it
                    errorMessage = ""
                },
                label = "Harga per Kg (Rp)",
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card Estimasi Saldo
            SiramCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Total Estimasi Saldo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(totalEstimasi),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Catatan
            SiramTextField(
                value = catatan,
                onValueChange = { catatan = it },
                label = "Catatan"
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Simpan
            Button(
                onClick = {
                    when {
                        jenisSampah.isBlank() -> errorMessage = "Jenis sampah tidak boleh kosong"
                        beratDouble <= 0 -> errorMessage = "Berat harus lebih dari 0 Kg"
                        else -> {
                            val sampahDiubah = sampah.copy(
                                jenisSampah = jenisSampah.trim(),
                                berat = beratDouble,
                                hargaPerKg = hargaDouble,
                                totalNilai = totalEstimasi,
                                catatan = catatan.trim()
                            )
                            onSimpanClick(sampahDiubah)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "💾 SIMPAN PERUBAHAN",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}