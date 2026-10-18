package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import java.text.NumberFormat
import java.util.Locale

private val BackgroundLight = Color(0xFFDCF0E0)
private val ButterYellow = Color(0xFFF7CF68)
private val ButterYellowDark = Color(0xFF2E2405)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatJemputScreen(
    viewModel: SampahViewModel,
    onTambahJemputClick: () -> Unit = {},
    onTrackClick: (PickupTransaction) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val daftarPickup by viewModel.semuaPenjemputan.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Pantau Penjemputan", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(start = 12.dp).size(40.dp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF0F172A))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onTambahJemputClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = ButterYellowDark) },
                text = { Text("Ajukan Jemput", fontWeight = FontWeight.Bold, color = ButterYellowDark) },
                containerColor = ButterYellow,
                shape = RoundedCornerShape(50.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            if (daftarPickup.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada riwayat penjemputan sampah.",
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(daftarPickup) { pickup ->
                        UserPickupStatusCard(pickup = pickup, onTrackClick = { onTrackClick(pickup) })
                    }
                }
            }
        }
    }
}

@Composable
private fun UserPickupStatusCard(pickup: PickupTransaction, onTrackClick: (PickupTransaction) -> Unit) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val (statusColor, statusBg, statusText) = when (pickup.status.uppercase()) {
        "SELESAI" -> Triple(Color(0xFF16A34A), Color(0xFFDCFCE7), "✅ SELESAI & SALDO MASUK")
        "DIPROSES" -> Triple(Color(0xFF0284C7), Color(0xFFE0F2FE), "🚚 SEDANG DIPROSES KURIR")
        "DITOLAK" -> Triple(Color(0xFFDC2626), Color(0xFFFEE2E2), "❌ PENJEMPUTAN DITOLAK")
        else -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), "🕒 MENUNGGU KONFIRMASI")
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pickup.jenisSampah,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (pickup.beratAktualKg > 0) "Berat Hasil Timbangan: ${pickup.beratAktualKg} Kg" else "Estimasi Berat: ${pickup.estimasiBeratKg} Kg",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "📍 Alamat Penjemputan: ${pickup.alamatLengkap}",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            if (pickup.status.uppercase() == "DIPROSES") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onTrackClick(pickup) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButterYellow,
                        contentColor = ButterYellowDark
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LACAK POSISI KURIR DI PETA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (pickup.status.uppercase() == "SELESAI" && pickup.totalNominalRp > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saldo Masuk:", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                        Text(
                            text = currencyFormat.format(pickup.totalNominalRp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
        }
    }
}
