package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RiwayatSampahScreen(
    viewModel: SampahViewModel,
    onEditClick: (Sampah) -> Unit = {}
) {

    val semuaSampah by viewModel.semuaSampah.collectAsState(
        initial = emptyList()
    )

    val totalBerat =
        semuaSampah.sumOf { it.berat }

    var sampahYangAkanDihapus by remember {
        mutableStateOf<Sampah?>(null)
    }

    // =====================================
    // DIALOG KONFIRMASI HAPUS
    // =====================================

    sampahYangAkanDihapus?.let { sampah ->

        AlertDialog(
            onDismissRequest = {
                sampahYangAkanDihapus = null
            },

            title = {
                Text(
                    text = "Hapus data?"
                )
            },

            text = {
                Text(
                    text =
                        "Data ${sampah.jenisSampah} akan dihapus dari perangkat dan Firebase. " +
                                "Tindakan ini tidak dapat dibatalkan."
                )
            },

            confirmButton = {

                Button(
                    onClick = {

                        viewModel.hapusSampah(sampah)

                        sampahYangAkanDihapus = null
                    },

                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.error
                    )
                ) {

                    Text("Hapus")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        sampahYangAkanDihapus = null
                    }
                ) {

                    Text("Batal")
                }
            }
        )
    }

    // =====================================
    // HALAMAN UTAMA
    // =====================================

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
            text = "Riwayat Sampah",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text =
                "Pantau dan kelola seluruh catatan sampah Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =====================================
        // RINGKASAN DATA
        // =====================================

        SiramCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "RINGKASAN DATA",
                        style =
                            MaterialTheme.typography.labelMedium,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.primary
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Total Catatan",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "${semuaSampah.size}",
                                fontSize = 26.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Total Berat",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "${String.format("%.1f", totalBerat)} Kg",
                                fontSize = 26.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    MaterialTheme.colorScheme
                                        .primary
                            )
                        }
                    }
                }

                Text(
                    text = "♻️",
                    fontSize = 48.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =====================================
        // JUDUL DAFTAR
        // =====================================

        Text(
            text = "Semua Aktivitas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // =====================================
        // EMPTY STATE
        // =====================================

        if (semuaSampah.isEmpty()) {

            SiramCard(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 36.dp,
                            horizontal = 20.dp
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "♻️",
                        fontSize = 52.sp
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = "Belum ada data sampah",
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
                            "Mulai catat sampah untuk melihat riwayat aktivitas Anda.",
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

            // =====================================
            // LIST DATA
            // =====================================

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = semuaSampah,
                    key = { sampah ->
                        sampah.id
                    }
                ) { sampah ->

                    RiwayatSampahItem(
                        sampah = sampah,

                        onEditClick = {
                            onEditClick(sampah)
                        },

                        onHapusClick = {
                            sampahYangAkanDihapus =
                                sampah
                        }
                    )
                }

                item {

                    Spacer(
                        modifier =
                            Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}


// =====================================
// ITEM RIWAYAT
// =====================================

@Composable
private fun RiwayatSampahItem(
    sampah: Sampah,
    onEditClick: () -> Unit,
    onHapusClick: () -> Unit
) {

    val formatTanggal =
        remember {
            SimpleDateFormat(
                "dd MMM yyyy • HH:mm",
                Locale("id", "ID")
            )
        }

    val tanggal =
        remember(sampah.tanggal) {
            formatTanggal.format(
                Date(sampah.tanggal)
            )
        }

    SiramCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            // =====================================
            // HEADER ITEM
            // =====================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "♻️",
                    fontSize = 34.sp
                )

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            sampah.jenisSampah,
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text = tanggal,
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                Text(
                    text =
                        "${String.format("%.1f", sampah.berat)} Kg",
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme
                            .primary
                )
            }

            // =====================================
            // CATATAN
            // =====================================

            if (sampah.catatan.isNotBlank()) {

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(
                    text = "Catatan",
                    style =
                        MaterialTheme.typography
                            .labelMedium,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        sampah.catatan,
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    maxLines = 3,
                    overflow =
                        TextOverflow.Ellipsis,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            // =====================================
            // TOMBOL AKSI
            // =====================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                OutlinedButton(
                    onClick =
                        onEditClick
                ) {

                    Text(
                        text = "Edit"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                TextButton(
                    onClick =
                        onHapusClick,
                    colors =
                        ButtonDefaults
                            .textButtonColors(
                                contentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                            )
                ) {

                    Text(
                        text = "Hapus"
                    )
                }
            }
        }
    }
}