package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.HargaSampah
import java.text.NumberFormat
import java.util.Locale

private val PrimaryGreen = Color(0xFF006C4C)
private val BackgroundLight = Color(0xFFDCF0E0)
private val CardBackground = Color(0xFFFFFFFF)
private val ButterYellow = Color(0xFFF7CF68)
private val ButterYellowDark = Color(0xFF2E2405)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPriceScreen(
    viewModel: SampahViewModel,
    onBackClick: () -> Unit = {}
) {
    val daftarHarga by viewModel.semuaHargaSampah.collectAsState(initial = emptyList())
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    var selectedItemForEdit by remember { mutableStateOf<HargaSampah?>(null) }
    var inputHargaBaru by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<HargaSampah?>(null) }

    // Dialog Konfirmasi Hapus Kategori Harga
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hapus Kategori ${item.jenisSampah}?", fontWeight = FontWeight.Bold) },
            text = { Text("Kategori sampah ini akan dihapus dari daftar tarif harga secara permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.hapusHargaSampah(item.jenisSampah)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("HAPUS")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("BATAL") }
            }
        )
    }

    // State untuk Dialog Tambah Kategori Sampah Baru (Random)
    var showAddDialog by remember { mutableStateOf(false) }
    var inputJenisBaru by remember { mutableStateOf("") }
    var inputHargaBaruCustom by remember { mutableStateOf("") }

    // Dialog Edit Harga
    selectedItemForEdit?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemForEdit = null },
            title = { Text("Ubah Tarif ${item.jenisSampah}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Masukkan harga baru per kilogram:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputHargaBaru,
                        onValueChange = { inputHargaBaru = it },
                        label = { Text("Harga per Kg (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hargaDouble = inputHargaBaru.toDoubleOrNull()
                        if (hargaDouble != null) {
                            viewModel.updateHargaSampah(item.jenisSampah, hargaDouble)
                            selectedItemForEdit = null
                            inputHargaBaru = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("SIMPAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForEdit = null }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Dialog Tambah Jenis Sampah Baru / Random
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Kategori Sampah Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Masukkan nama jenis sampah dan tarif per kilogramnya:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = inputJenisBaru,
                        onValueChange = { inputJenisBaru = it },
                        label = { Text("Nama Jenis Sampah (Contoh: Besi Tua, Minyak Jelantah)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputHargaBaruCustom,
                        onValueChange = { inputHargaBaruCustom = it },
                        label = { Text("Harga per Kg (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hargaDouble = inputHargaBaruCustom.toDoubleOrNull()
                        if (inputJenisBaru.isNotBlank() && hargaDouble != null) {
                            viewModel.updateHargaSampah(inputJenisBaru.trim(), hargaDouble)
                            showAddDialog = false
                            inputJenisBaru = ""
                            inputHargaBaruCustom = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("TAMBAH")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    inputJenisBaru = ""
                    inputHargaBaruCustom = ""
                }) {
                    Text("BATAL")
                }
            }
        )
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Kelola Tarif Harga Sampah", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A)) },
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
                actions = {
                    // Tombol Tambah Kategori Sampah Random/Baru di Pojok Kanan Atas
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(end = 12.dp).size(40.dp)
                    ) {
                        IconButton(
                            onClick = { showAddDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Sampah Baru", tint = Color(0xFF0F172A))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
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
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daftar Harga Beli per Kilogram",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Atur harga jenis standar atau tambahkan kategori sampah random baru.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButterYellow,
                            contentColor = ButterYellowDark
                        ),
                        shape = RoundedCornerShape(50.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Baru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (daftarHarga.isEmpty()) {
                    val defaultCategories = listOf("Plastik", "Kertas", "Logam", "Kaca", "Organik")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(defaultCategories) { kategori ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text(text = kategori, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(text = "Belum diatur", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            selectedItemForEdit = HargaSampah(jenisSampah = kategori, hargaPerKg = 0.0)
                                            inputHargaBaru = ""
                                        }
                                    ) {
                                        Text("Atur Harga")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(daftarHarga, key = { it.jenisSampah }) { item ->
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryGreen.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = item.jenisSampah,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = currencyFormat.format(item.hargaPerKg) + " / Kg",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryGreen
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedItemForEdit = item
                                                inputHargaBaru = item.hargaPerKg.toInt().toString()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Ubah", fontSize = 12.sp)
                                        }

                                        IconButton(
                                            onClick = { itemToDelete = item },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFEE2E2))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}