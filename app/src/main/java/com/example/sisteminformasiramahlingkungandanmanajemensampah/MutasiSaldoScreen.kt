package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BackgroundLight = Color(0xFFDCF0E0)
private val CardBackground = Color(0xFFFFFFFF)
private val GreenSuccess = Color(0xFF16A34A)
private val GreenSuccessBg = Color(0xFFDCFCE7)
private val RedDanger = Color(0xFFDC2626)
private val RedDangerBg = Color(0xFFFEE2E2)

data class MutasiItem(
    val id: String,
    val tanggal: Long,
    val keterangan: String,
    val nominal: Double,
    val isMasuk: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutasiSaldoScreen(
    viewModel: SampahViewModel,
    onBackClick: () -> Unit = {}
) {
    val semuaSampah by viewModel.semuaSampah.collectAsState(initial = emptyList())
    val semuaPenarikan by viewModel.semuaPenarikanUser.collectAsState(initial = emptyList())

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))

    val mutasiList = remember(semuaSampah, semuaPenarikan) {
        val list = mutableListOf<MutasiItem>()
        semuaSampah.forEach { s ->
            if (s.status.uppercase() == "DISETUJUI") {
                list.add(
                    MutasiItem(
                        id = "sampah_${s.id}",
                        tanggal = s.tanggal,
                        keterangan = "Setoran Sampah: ${s.jenisSampah} (${s.berat} Kg)",
                        nominal = s.totalNilai,
                        isMasuk = true
                    )
                )
            }
        }
        semuaPenarikan.forEach { p ->
            list.add(
                MutasiItem(
                    id = "tarik_${p.id}",
                    tanggal = p.tanggal,
                    keterangan = "Tarik Saldo (${p.metodePenarikan} - ${p.nomorTujuan})",
                    nominal = p.nominalRp,
                    isMasuk = false
                )
            )
        }
        list.sortedByDescending { it.tanggal }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Mutasi Saldo & Rekening", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(start = 12.dp).size(40.dp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF0F172A))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            if (mutasiList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada riwayat mutasi saldo.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(mutasiList, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (item.isMasuk) GreenSuccessBg else RedDangerBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.isMasuk) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (item.isMasuk) GreenSuccess else RedDanger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.keterangan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateFormat.format(Date(item.tanggal)),
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Text(
                                    text = (if (item.isMasuk) "+ " else "- ") + currencyFormat.format(item.nominal),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (item.isMasuk) GreenSuccess else RedDanger
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
