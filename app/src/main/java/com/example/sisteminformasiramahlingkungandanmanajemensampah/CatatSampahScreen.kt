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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Save
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

private val OrangePending = Color(0xFFD97706)
private val OrangePendingBg = Color(0xFFFEF3C7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatSampahScreen(
    viewModel: SampahViewModel,
    jenisAwal: String = "",
    onSimpanClick: () -> Unit = {}
) {
    var jenisSampah by remember(jenisAwal) { mutableStateOf(jenisAwal) }
    var berat by remember { mutableStateOf("") }
    var hargaPerKgText by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    LaunchedEffect(jenisSampah) {
        if (jenisSampah.isNotBlank()) {
            val hg = viewModel.getHargaRiilByJenis(jenisSampah)
            hargaPerKgText = if (hg > 0) hg.toInt().toString() else ""
        }
    }

    val beratDouble = berat.toDoubleOrNull() ?: 0.0
    val hargaDouble = hargaPerKgText.toDoubleOrNull() ?: 0.0
    val totalEstimasi = beratDouble * hargaDouble

    val scrollState = rememberScrollState()
    val daftarHarga by viewModel.semuaHargaSampah.collectAsState(initial = emptyList())
    val rekomendasiKategori = if (daftarHarga.isNotEmpty()) {
        daftarHarga.map { it.jenisSampah }
    } else {
        listOf("Plastik", "Kardus", "Kertas", "Kaleng", "Logam")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catat Sampah", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onSimpanClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground,
                    titleContentColor = Color(0xFF0F172A)
                )
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
                // BANNER HEADER GRADIENT
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryGreen, PrimaryGradient)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Recycling,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Form Input Sampah Mandiri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Data sampah akan dikirim ke Admin untuk diverifikasi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    // INFO STATUS VALIDASI ADMIN
                    Surface(
                        color = OrangePendingBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = OrangePending,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Inputan baru akan berstatus MENUNGGU VALIDASI sebelum saldo terverifikasi masuk ke akun Anda.",
                                fontSize = 11.sp,
                                color = OrangePending,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // KARTU FORM UTAMA
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
                            Text(
                                text = "Detail Sampah & Timbangan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )

                            // CHIP PILIHAN CEPAT KATEGORI (MENGGUNAKAN LAZYROW AGAR TIDAK NGEBUG)
                            Text("Pilih Kategori Cepat:", fontSize = 12.sp, color = Color(0xFF64748B))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(rekomendasiKategori) { kat ->
                                    FilterChip(
                                        selected = jenisSampah.equals(kat, ignoreCase = true),
                                        onClick = {
                                            jenisSampah = kat
                                            errorMessage = ""
                                        },
                                        label = { Text(kat, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }

                            // JENIS SAMPAH TEXT INPUT
                            OutlinedTextField(
                                value = jenisSampah,
                                onValueChange = {
                                    jenisSampah = it
                                    errorMessage = ""
                                },
                                label = { Text("Jenis Sampah") },
                                placeholder = { Text("Contoh: Botol Plastik, Kardus Bekas") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // BERAT SAMPAH
                            OutlinedTextField(
                                value = berat,
                                onValueChange = {
                                    berat = it
                                    errorMessage = ""
                                },
                                label = { Text("Berat Sampah (Kg)") },
                                placeholder = { Text("Contoh: 2.5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // HARGA PER KG
                            OutlinedTextField(
                                value = hargaPerKgText,
                                onValueChange = {
                                    hargaPerKgText = it
                                    errorMessage = ""
                                },
                                label = { Text("Harga Beli per Kg (Rp)") },
                                placeholder = { Text("Terisi otomatis atau sesuaikan") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // CATATAN TAMBAHAN
                            OutlinedTextField(
                                value = catatan,
                                onValueChange = { catatan = it },
                                label = { Text("Catatan Opsional") },
                                placeholder = { Text("Kondisi sampah, lokasi penyerahan, dll.") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // CARD ESTIMASI SALDO REAL-TIME
                            Surface(
                                color = PrimaryGreen.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Estimasi Saldo",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            Text(
                                                text = currencyFormat.format(totalEstimasi),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryGreen
                                            )
                                        }
                                    }
                                }
                            }

                            // PESAN ERROR JIKA INPUTAN INVALID
                            AnimatedVisibility(
                                visible = errorMessage.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // TOMBOL SIMPAN DATA
                            Button(
                                onClick = {
                                    when {
                                        jenisSampah.isBlank() -> {
                                            errorMessage = "⚠️ Jenis sampah wajib diisi"
                                        }

                                        berat.isBlank() -> {
                                            errorMessage = "⚠️ Berat sampah wajib diisi"
                                        }

                                        berat.toDoubleOrNull() == null -> {
                                            errorMessage = "⚠️ Berat harus berupa angka"
                                        }

                                        berat.toDouble() <= 0 -> {
                                            errorMessage = "⚠️ Berat harus lebih dari 0 Kg"
                                        }

                                        else -> {
                                            val hargaFix = hargaPerKgText.toDoubleOrNull()

                                            viewModel.simpanSampah(
                                                jenisSampah = jenisSampah.trim(),
                                                berat = berat.toDouble(),
                                                catatan = catatan.trim(),
                                                hargaPerKgCustom = hargaFix
                                            )

                                            onSimpanClick()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SIMPAN DATA SAMPAH",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // INFO FOOTER
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Harga per Kg terisi otomatis sesuai standar bank sampah. Anda dapat menyesuaikannya secara manual.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}