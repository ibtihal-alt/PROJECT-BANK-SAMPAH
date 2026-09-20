package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale

// PALET WARNA KOMERSIAL (LIGHT THEME)
private val PrimaryGreen = Color(0xFF006C4C)
private val PrimaryGradient = Color(0xFF00A86B)
private val BackgroundLight = Color(0xFFF8FAFC)
private val CardBackground = Color(0xFFFFFFFF)

private val OrangePending = Color(0xFFD97706)
private val OrangePendingBg = Color(0xFFFEF3C7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JemputSampahScreen(
    viewModel: SampahViewModel,
    onAjukanClick: (jenis: String, berat: Double, alamat: String, hp: String, catatan: String, lat: Double, lng: Double) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }

    var jenisSampah by remember { mutableStateOf("") }
    var estimasiBerat by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf(userPrefs.alamatDefault) }
    var nomorHp by remember { mutableStateOf(userPrefs.nomorHp) }
    var catatan by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var pickupLat by remember { mutableStateOf(0.0) }
    var pickupLng by remember { mutableStateOf(0.0) }
    var showMapPicker by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { loc ->
                    if (loc != null) {
                        pickupLat = loc.latitude
                        pickupLng = loc.longitude
                        var addressText = ""
                        try {
                            val geocoder = Geocoder(context, Locale("id", "ID"))
                            val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                addressText = addresses[0].getAddressLine(0) ?: ""
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                        if (addressText.isNotBlank()) {
                            alamat = addressText
                        }
                        Toast.makeText(context, "📍 GPS Ditemukan!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                // ignore
            }
        }
    }

    val scrollState = rememberScrollState()
    val daftarHarga by viewModel.semuaHargaSampah.collectAsState(initial = emptyList())
    val rekomendasiKategori = if (daftarHarga.isNotEmpty()) {
        daftarHarga.map { it.jenisSampah }
    } else {
        listOf("Plastik", "Kardus", "Kertas", "Kaleng", "Campuran")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengajuan Penjemputan", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Layanan Jemput Ke Rumah",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tim kurir SIRAM akan menjemput sampah langsung di lokasi Anda.",
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
                                text = "Permintaan akan diproses Admin & jadwal armada akan dikirim via WhatsApp.",
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
                                text = "Form Rincian Penjemputan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )

                            // CHIP PILIHAN CEPAT KATEGORI
                            Text("Pilih Jenis Sampah Cepat:", fontSize = 12.sp, color = Color(0xFF64748B))
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
                                placeholder = { Text("Contoh: Plastik & Kardus Campur") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // ESTIMASI BERAT
                            OutlinedTextField(
                                value = estimasiBerat,
                                onValueChange = {
                                    estimasiBerat = it
                                    errorMessage = ""
                                },
                                label = { Text("Perkiraan Berat (Kg)") },
                                placeholder = { Text("Contoh: 5.0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // PENENTUAN TITIK LOKASI JEMPUT (SHOPEE STYLE)
                            Text("Titik Penjemputan Peta / GPS:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            try {
                                                fusedLocationClient.getCurrentLocation(
                                                    Priority.PRIORITY_HIGH_ACCURACY,
                                                    CancellationTokenSource().token
                                                ).addOnSuccessListener { loc ->
                                                    if (loc != null) {
                                                        pickupLat = loc.latitude
                                                        pickupLng = loc.longitude
                                                        var addressText = ""
                                                        try {
                                                            val geocoder = Geocoder(context, Locale("id", "ID"))
                                                            val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                                            if (!addresses.isNullOrEmpty()) {
                                                                addressText = addresses[0].getAddressLine(0) ?: ""
                                                            }
                                                        } catch (e: Exception) {
                                                            // ignore
                                                        }
                                                        if (addressText.isNotBlank()) {
                                                            alamat = addressText
                                                        }
                                                        Toast.makeText(context, "📍 GPS Ditemukan!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (e: SecurityException) {
                                                // ignore
                                            }
                                        } else {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GPS Saya", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showMapPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pilih di Peta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // ALAMAT LENGKAP
                            OutlinedTextField(
                                value = alamat,
                                onValueChange = {
                                    alamat = it
                                    errorMessage = ""
                                },
                                label = { Text("Alamat Penjemputan Lengkap") },
                                placeholder = { Text("Masukkan jalan, RT/RW, nomor rumah") },
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // NOMOR WHATSAPP / HP
                            OutlinedTextField(
                                value = nomorHp,
                                onValueChange = {
                                    nomorHp = it
                                    errorMessage = ""
                                },
                                label = { Text("Nomor WhatsApp / HP Active") },
                                placeholder = { Text("Contoh: 081234567890") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                                label = { Text("Catatan Lokasi (Opsional)") },
                                placeholder = { Text("Contoh: Pagar rumah warna hijau, patokan samping masjid") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

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

                            // TOMBOL SUBMIT PENJEMPUTAN
                            Button(
                                onClick = {
                                    when {
                                        jenisSampah.isBlank() -> errorMessage = "⚠️ Jenis sampah wajib diisi"
                                        estimasiBerat.isBlank() -> errorMessage = "⚠️ Berat estimasi wajib diisi"
                                        estimasiBerat.toDoubleOrNull() == null -> errorMessage = "⚠️ Berat harus berupa angka"
                                        estimasiBerat.toDouble() <= 0 -> errorMessage = "⚠️ Berat minimal 0.5 Kg"
                                        alamat.isBlank() -> errorMessage = "⚠️ Alamat penjemputan wajib diisi"
                                        nomorHp.isBlank() -> errorMessage = "⚠️ Nomor WhatsApp/HP wajib diisi"
                                        else -> {
                                            onAjukanClick(
                                                jenisSampah.trim(),
                                                estimasiBerat.toDouble(),
                                                alamat.trim(),
                                                nomorHp.trim(),
                                                catatan.trim(),
                                                pickupLat,
                                                pickupLng
                                            )
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
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AJUKAN PENJEMPUTAN",
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
                                text = "Pastikan nomor HP aktif. Timbangan aktual dan penentuan nominal akhir akan dikonfirmasi ulang oleh kurir saat di lokasi.",
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

    if (showMapPicker) {
        MapPickerModal(
            initialLat = pickupLat,
            initialLng = pickupLng,
            onConfirmLocation = { lat, lng, address ->
                pickupLat = lat
                pickupLng = lng
                if (address.isNotBlank()) {
                    alamat = address
                }
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapPickerModal(
    initialLat: Double,
    initialLng: Double,
    onConfirmLocation: (lat: Double, lng: Double, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val startLat = if (initialLat != 0.0) initialLat else -7.4029
    val startLng = if (initialLng != 0.0) initialLng else 111.4452

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(startLat, startLng), 16f)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Geser Peta Pilih Titik Jemput", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Tutup")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState
                    )

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PinDrop,
                                contentDescription = "Titik Terpilih",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(42.dp)
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "Titik Jemput Anda",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(21.dp))
                        }
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val currentTarget = cameraPositionState.position.target
                        Text(
                            text = "📍 Lokasi Terpilih:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("Lat: %.5f, Lng: %.5f", currentTarget.latitude, currentTarget.longitude),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val lat = currentTarget.latitude
                                val lng = currentTarget.longitude
                                var addressText = ""
                                try {
                                    val geocoder = Geocoder(context, Locale("id", "ID"))
                                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        val addr = addresses[0]
                                        addressText = addr.getAddressLine(0) ?: ""
                                    }
                                } catch (e: Exception) {
                                    // ignore
                                }
                                if (addressText.isBlank()) {
                                    addressText = "Titik Jemput Peta (Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)})"
                                }
                                onConfirmLocation(lat, lng, addressText)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C4C))
                        ) {
                            Text("GUNAKAN TITIK LOKASI INI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}