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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatistikScreen(
    viewModel: SampahViewModel
) {

    val totalBerat by viewModel.totalBerat.collectAsState(
        initial = 0.0
    )

    val jumlahSampah by viewModel.jumlahSampah.collectAsState(
        initial = 0
    )

    val jenisTerbanyak by viewModel.jenisTerbanyak.collectAsState(
        initial = null
    )

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
            text = "Statistik Sampah",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text =
                "Pantau ringkasan aktivitas dan data sampah yang telah Anda kelola.",
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
        // TOTAL SAMPAH
        // =====================================

        SiramCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "TOTAL SAMPAH DIKELOLA",
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
                        Modifier.height(12.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.Bottom
                ) {

                    Text(
                        text =
                            String.format(
                                "%.1f",
                                totalBerat
                            ),
                        fontSize = 42.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme
                                .primary
                    )

                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )

                    Text(
                        text = "Kg",
                        fontSize = 20.sp,
                        modifier =
                            Modifier.padding(
                                bottom = 8.dp
                            ),
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Akumulasi seluruh berat sampah yang telah dicatat.",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =====================================
        // JUMLAH CATATAN DAN JENIS TERBANYAK
        // =====================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // JUMLAH CATATAN

            SiramCard(
                modifier =
                    Modifier.weight(1f)
            ) {

                Column {

                    Text(
                        text = "Jumlah Catatan",
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            jumlahSampah.toString(),
                        fontSize = 32.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme
                                .primary
                    )

                    Text(
                        text = "Data tersimpan",
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            // JENIS SAMPAH TERBANYAK

            SiramCard(
                modifier =
                    Modifier.weight(1f)
            ) {

                Column {

                    Text(
                        text = "Jenis Terbanyak",
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            jenisTerbanyak
                                ?: "-",
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold,
                        maxLines = 2,
                        color =
                            MaterialTheme.colorScheme
                                .primary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            if (jenisTerbanyak == null) {
                                "Belum ada data"
                            } else {
                                "Berdasarkan total berat"
                            },
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

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )

        // =====================================
        // RINGKASAN
        // =====================================

        Text(
            text = "Ringkasan",
            style =
                MaterialTheme.typography
                    .titleLarge,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        SiramCard(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column {

                StatistikRingkasanItem(
                    title = "Total berat sampah",
                    value =
                        "${String.format("%.1f", totalBerat)} Kg"
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                StatistikRingkasanItem(
                    title = "Jumlah aktivitas tercatat",
                    value =
                        "$jumlahSampah aktivitas"
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                StatistikRingkasanItem(
                    title = "Kategori sampah dominan",
                    value =
                        jenisTerbanyak
                            ?: "Belum ada data"
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        // =====================================
        // PESAN KOSONG / INFORMASI
        // =====================================

        if (jumlahSampah == 0) {

            SiramCard(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Belum ada data statistik",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Mulai catat sampah agar aplikasi dapat menampilkan statistik pengelolaan Anda.",
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
}


// =====================================
// ITEM RINGKASAN
// =====================================

@Composable
private fun StatistikRingkasanItem(
    title: String,
    value: String
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style =
                    MaterialTheme.typography
                        .bodyMedium
            )
        }

        Text(
            text = value,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme.colorScheme
                    .primary
        )
    }
}