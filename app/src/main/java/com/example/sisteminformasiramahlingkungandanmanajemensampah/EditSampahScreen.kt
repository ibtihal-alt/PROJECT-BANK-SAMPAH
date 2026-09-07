package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah

@Composable
fun EditSampahScreen(
    sampah: Sampah,
    onSimpanClick: (Sampah) -> Unit
) {

    var jenisSampah by remember {
        mutableStateOf(sampah.jenisSampah)
    }

    var berat by remember {
        mutableStateOf(sampah.berat.toString())
    }

    var catatan by remember {
        mutableStateOf(sampah.catatan)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            text = "Edit Data Sampah",
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = jenisSampah,
            onValueChange = { jenisSampah = it },
            label = { Text("Jenis Sampah") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = berat,
            onValueChange = { berat = it },
            label = { Text("Berat (Kg)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = catatan,
            onValueChange = { catatan = it },
            label = { Text("Catatan") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {

                val beratBaru =
                    berat.toDoubleOrNull() ?: 0.0

                if (
                    jenisSampah.isNotBlank() &&
                    beratBaru > 0
                ) {

                    val sampahBaru = sampah.copy(
                        jenisSampah = jenisSampah,
                        berat = beratBaru,
                        catatan = catatan
                    )

                    onSimpanClick(sampahBaru)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SIMPAN PERUBAHAN")
        }
    }
}