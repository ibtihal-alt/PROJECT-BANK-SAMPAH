package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(
    viewModel: SampahViewModel,
    onCatatSampahClick: () -> Unit = {},
    onAiScannerClick: () -> Unit = {},
    onStatistikClick: () -> Unit = {},
    onRiwayatClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {

    val totalBerat by viewModel.totalBerat.collectAsState(
        initial = 0.0
    )

    val semuaSampah by viewModel.semuaSampah.collectAsState(
        initial = emptyList()
    )

    val aktivitasTerbaru = semuaSampah.take(3)

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
            text = "Halo, Pengguna 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "Mari kelola sampah dan jaga lingkungan bersama.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // =========================
        // TOTAL SAMPAH
        // =========================
        SiramCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "TOTAL SAMPAH DIKELOLA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                verticalAlignment = Alignment.Bottom
            ) {

                Text(
                    text = String.format(
                        "%.1f",
                        totalBerat
                    ),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = "Kg",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(
                        bottom = 8.dp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Total sampah yang telah kamu catat dan kelola.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =========================
        // FITUR UTAMA
        // =========================
        Text(
            text = "Fitur Utama",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            DashboardFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Recycling,
                title = "Catat Sampah",
                subtitle = "Tambah data",
                onClick = onCatatSampahClick
            )

            DashboardFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AutoAwesome,
                title = "AI Scanner",
                subtitle = "Deteksi sampah",
                onClick = onAiScannerClick
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        DashboardMenuCard(
            icon = Icons.Default.Analytics,
            title = "Statistik Sampah",
            subtitle = "Lihat ringkasan data sampah",
            onClick = onStatistikClick
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        DashboardMenuCard(
            icon = Icons.Default.History,
            title = "Riwayat Sampah",
            subtitle = "Lihat dan kelola semua data",
            onClick = onRiwayatClick
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =========================
        // AKTIVITAS TERBARU
        // =========================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Aktivitas Terbaru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (semuaSampah.isNotEmpty()) {

                TextButton(
                    onClick = onRiwayatClick
                ) {

                    Text(
                        text = "Lihat Semua"
                    )

                    Icon(
                        imageVector =
                            Icons.Default.ArrowForward,
                        contentDescription =
                            "Lihat semua riwayat"
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (aktivitasTerbaru.isEmpty()) {

            SiramCard(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,
                        contentDescription = null,
                        tint =
                            MaterialTheme.colorScheme.primary
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Belum ada aktivitas",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Mulai catat sampah untuk melihat aktivitas terbaru.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

        } else {

            aktivitasTerbaru.forEach { sampah ->

                DashboardActivityItem(
                    jenisSampah = sampah.jenisSampah,
                    berat = sampah.berat,
                    catatan = sampah.catatan
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        // =========================
        // LOGOUT
        // =========================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onLogoutClick()
                },
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.errorContainer
            ),
            shape = MaterialTheme.shapes.large
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint =
                        MaterialTheme.colorScheme.error
                )

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column {

                    Text(
                        text = "Keluar dari akun",
                        fontWeight = FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.error
                    )

                    Text(
                        text =
                            "Keluar dan kembali ke halaman login",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onErrorContainer
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }
}

// =====================================
// FEATURE CARD
// =====================================
@Composable
private fun DashboardFeatureCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .clickable {
                onClick()
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme
                    .primaryContainer
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint =
                    MaterialTheme.colorScheme
                        .primary
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

// =====================================
// MENU CARD
// =====================================
@Composable
private fun DashboardMenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = subtitle,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Icon(
                imageVector =
                    Icons.Default.ArrowForward,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

// =====================================
// ACTIVITY ITEM
// =====================================
@Composable
private fun DashboardActivityItem(
    jenisSampah: String,
    berat: Double,
    catatan: String
) {

    SiramCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "♻️",
                fontSize = 28.sp
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = jenisSampah,
                    fontWeight = FontWeight.Bold
                )

                if (catatan.isNotBlank()) {

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text = catatan,
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Text(
                text =
                    "${String.format("%.1f", berat)} Kg",
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}