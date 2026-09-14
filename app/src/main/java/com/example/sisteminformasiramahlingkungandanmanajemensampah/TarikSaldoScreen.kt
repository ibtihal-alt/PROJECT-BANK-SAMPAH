package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val PrimaryGreen = Color(0xFF006C4C)
private val PrimaryGradient = Color(0xFF00A86B)
private val BackgroundLight = Color(0xFFF8FAFC)
private val CardBackground = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarikSaldoScreen(
    saldoTersedia: Double,
    onAjukanTarikClick: (metode: String, nomor: String, nama: String, nominal: Double) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var metodePenarikan by remember { mutableStateOf("Gopay") }
    var nomorTujuan by remember { mutableStateOf("") }
    var namaPemilik by remember { mutableStateOf("") }
    var nominalText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val daftarMetode = listOf("Gopay", "DANA", "OVO", "ShopeePay", "BCA", "BRI", "Mandiri")
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pencairan Saldo", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBackground)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // BANNER SALDO AKTIF
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(Brush.horizontalGradient(listOf(PrimaryGreen, PrimaryGradient)))
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column {
                        Text("Saldo Siap Ditarik", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        Text(
                            text = currencyFormat.format(saldoTersedia),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Pilih Bank / E-Wallet Tujuan", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(daftarMetode) { m ->
                                    FilterChip(
                                        selected = metodePenarikan == m,
                                        onClick = { metodePenarikan = m },
                                        label = { Text(m, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = nomorTujuan,
                                onValueChange = { nomorTujuan = it; errorMessage = "" },
                                label = { Text("Nomor HP / Rekening $metodePenarikan") },
                                placeholder = { Text("Contoh: 081234567890") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = namaPemilik,
                                onValueChange = { namaPemilik = it; errorMessage = "" },
                                label = { Text("Nama Pemilik Akun / Rekening") },
                                placeholder = { Text("Sesuai nama terdaftar di e-wallet/bank") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = nominalText,
                                onValueChange = { nominalText = it; errorMessage = "" },
                                label = { Text("Nominal Pencairan (Rp)") },
                                placeholder = { Text("Minimal Rp 10.000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    val nominalVal = nominalText.toDoubleOrNull() ?: 0.0
                                    when {
                                        nomorTujuan.isBlank() -> errorMessage = "⚠️ Nomor tujuan wajib diisi"
                                        namaPemilik.isBlank() -> errorMessage = "⚠️ Nama pemilik akun wajib diisi"
                                        nominalVal < 10000 -> errorMessage = "⚠️ Minimal pencairan saldo Rp 10.000"
                                        nominalVal > saldoTersedia -> errorMessage = "⚠️ Saldo Anda tidak mencukupi"
                                        else -> onAjukanTarikClick(metodePenarikan, nomorTujuan, namaPemilik, nominalVal)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AJUKAN PENCAIRAN SALDO", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                            Text(
                                "Proses pencairan akan diverifikasi Admin maksimal 1x24 jam kerja.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}