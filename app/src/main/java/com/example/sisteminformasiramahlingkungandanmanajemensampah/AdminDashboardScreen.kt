package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val PrimaryGreen = Color(0xFF006C4C)
private val PrimaryGradient = Color(0xFF00A86B)
private val BackgroundLight = Color(0xFFF8FAFC)
private val CardBackground = Color(0xFFFFFFFF)

private val OrangePending = Color(0xFFD97706)
private val OrangePendingBg = Color(0xFFFEF3C7)

private val BlueProcess = Color(0xFF0284C7)
private val BlueProcessBg = Color(0xFFE0F2FE)

private val GreenSuccess = Color(0xFF16A34A)
private val GreenSuccessBg = Color(0xFFDCFCE7)

private val RedDanger = Color(0xFFDC2626)
private val RedDangerBg = Color(0xFFFEE2E2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: SampahViewModel,
    onLogoutClick: () -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val fleetRepo = remember { FleetRepository() }
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            startKurirLocationUpdates(fusedLocationClient, auth, fleetRepo, coroutineScope)
        }
    }

    // Otomatis siarkan GPS di background untuk Kurir saat online
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startKurirLocationUpdates(fusedLocationClient, auth, fleetRepo, coroutineScope)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var selectedFilterPickup by remember { mutableStateOf("DIPROSES") }
    var searchQuery by remember { mutableStateOf("") }

    val daftarPickupAdmin by viewModel.semuaPenjemputanAdmin.collectAsState(initial = emptyList())
    var showSelesaiDialog by remember { mutableStateOf(false) }
    var selectedPickupForSelesai by remember { mutableStateOf<PickupTransaction?>(null) }

    val listStatusPickup = listOf("DIPROSES", "SELESAI", "SEMUA")

    val currentKurirUid = auth.currentUser?.uid ?: ""

    val totalTonaseSelesai = remember(daftarPickupAdmin, currentKurirUid) {
        daftarPickupAdmin.filter { it.status.equals("SELESAI", ignoreCase = true) && it.assignedKurirId == currentKurirUid }
            .sumOf { if (it.beratAktualKg > 0) it.beratAktualKg else it.estimasiBeratKg }
    }

    val totalNominalSelesai = remember(daftarPickupAdmin, currentKurirUid) {
        daftarPickupAdmin.filter { it.status.equals("SELESAI", ignoreCase = true) && it.assignedKurirId == currentKurirUid }
            .sumOf { it.totalNominalRp }
    }

    val filteredPickupList = remember(daftarPickupAdmin, selectedFilterPickup, searchQuery, currentKurirUid) {
        daftarPickupAdmin.filter { pickup ->
            // Kurir HANYA melihat penjemputan yang sudah ditugaskan secara eksplisit ke kurir tersebut oleh Super Admin
            val isForThisKurir = pickup.assignedKurirId == currentKurirUid
            val matchesTab = when (selectedFilterPickup) {
                "DIPROSES" -> pickup.status.equals("DIPROSES", ignoreCase = true) && isForThisKurir
                "SELESAI" -> pickup.status.equals("SELESAI", ignoreCase = true) && isForThisKurir
                else -> isForThisKurir
            }
            val matchesSearch = pickup.jenisSampah.contains(searchQuery, ignoreCase = true) ||
                    pickup.alamatLengkap.contains(searchQuery, ignoreCase = true) ||
                    pickup.nomorHp.contains(searchQuery, ignoreCase = true)
            isForThisKurir && matchesTab && matchesSearch
        }
    }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // HEADER BANNER KURIR (PERBAIKAN TATA LETAK & ICON LOGOUT)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🚚 Kurir Lapangan Hub",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tugas Penjemputan Ditugaskan",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onLogoutClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // RINGKASAN KINERJA KURIR
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Tonase Ditangani", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = String.format("%.1f Kg", totalTonaseSelesai),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(36.dp), color = Color(0xFFE2E8F0))
                        Column {
                            Text("Total Nilai Diselesaikan", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = currencyFormat.format(totalNominalSelesai),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueProcess
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari penjemputan, alamat, no hp...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // METRIC CARDS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfessionalMetricCard(
                        title = "Diproses",
                        count = daftarPickupAdmin.count { it.status.equals("DIPROSES", ignoreCase = true) && it.assignedKurirId == currentKurirUid },
                        accentColor = BlueProcess,
                        bgColor = BlueProcessBg,
                        icon = Icons.Default.LocalShipping,
                        modifier = Modifier.weight(1f)
                    )
                    ProfessionalMetricCard(
                        title = "Selesai",
                        count = daftarPickupAdmin.count { it.status.equals("SELESAI", ignoreCase = true) && it.assignedKurirId == currentKurirUid },
                        accentColor = GreenSuccess,
                        bgColor = GreenSuccessBg,
                        icon = Icons.Default.TaskAlt,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                ScrollableTabRow(
                    selectedTabIndex = listStatusPickup.indexOf(selectedFilterPickup),
                    edgePadding = 20.dp,
                    divider = {},
                    containerColor = Color.Transparent,
                    contentColor = PrimaryGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listStatusPickup.forEach { filter ->
                        Tab(
                            selected = selectedFilterPickup == filter,
                            onClick = { selectedFilterPickup = filter },
                            text = { Text(filter, fontWeight = FontWeight.Medium, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                AnimatedVisibility(visible = true) {
                    if (filteredPickupList.isEmpty()) {
                        EmptyStateView(message = "Belum ada tugas penjemputan dari Super Admin untuk Anda.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredPickupList, key = { it.id }) { pickup ->
                                CommercialPickupCard(
                                    pickup = pickup,
                                    onUpdateStatus = { item, statusBaru ->
                                        if (statusBaru == "SELESAI") {
                                            selectedPickupForSelesai = item
                                            showSelesaiDialog = true
                                        } else {
                                            viewModel.updateStatusPenjemputanByAdmin(item, statusBaru)
                                            selectedFilterPickup = statusBaru
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSelesaiDialog && selectedPickupForSelesai != null) {
        KalkulatorLapanganDialog(
            pickup = selectedPickupForSelesai!!,
            onDismiss = { showSelesaiDialog = false; selectedPickupForSelesai = null },
            onConfirmSelesai = { beratAktual, hargaPerKg, totalSaldo, fotoUri ->
                val pickupUpdated = selectedPickupForSelesai!!.copy(
                    beratAktualKg = beratAktual,
                    hargaPerKgAktual = hargaPerKg,
                    totalNominalRp = totalSaldo,
                    fotoBuktiUri = fotoUri,
                    status = "MENUNGGU_VERIFIKASI"
                )
                viewModel.updateStatusPenjemputanByAdmin(pickupUpdated, "MENUNGGU_VERIFIKASI")
                showSelesaiDialog = false
                selectedPickupForSelesai = null
            }
        )
    }
}

@Composable
private fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun ProfessionalMetricCard(
    title: String,
    count: Int,
    accentColor: Color,
    bgColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
                }
            }
            Text(text = "$count", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}

@Composable
private fun CommercialPickupCard(
    pickup: PickupTransaction,
    onUpdateStatus: (item: PickupTransaction, statusBaru: String) -> Unit
) {
    val context = LocalContext.current
    val (statusColor, statusBg) = when (pickup.status.uppercase()) {
        "SELESAI" -> GreenSuccess to GreenSuccessBg
        "DIPROSES" -> BlueProcess to BlueProcessBg
        "DITOLAK" -> RedDanger to RedDangerBg
        else -> OrangePending to OrangePendingBg
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(text = pickup.jenisSampah, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        Text(text = "Estimasi: ${pickup.estimasiBeratKg} Kg", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                    }
                }
                Surface(color = statusBg, shape = RoundedCornerShape(50.dp)) {
                    Text(text = pickup.status.uppercase(), fontWeight = FontWeight.Bold, color = statusColor, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "📍 Alamat: ${pickup.alamatLengkap}", fontSize = 13.sp, color = Color(0xFF334155))
            Text(text = "📞 No HP: ${pickup.nomorHp}", fontSize = 13.sp, color = Color(0xFF64748B))

            if (pickup.assignedKurirName.isNotBlank()) {
                Text(text = "👤 Ditugaskan ke: ${pickup.assignedKurirName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
            }

            if (pickup.fotoBuktiUri.isNotBlank()) {
                Text(text = "📸 Foto Bukti Penimbangan: Tersedia", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BlueProcess)
            }

            if (pickup.catatan.isNotBlank()) {
                Text(text = "Catatan: ${pickup.catatan}", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onUpdateStatus(pickup, "SELESAI") },
                        enabled = !pickup.status.equals("SELESAI", ignoreCase = true),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, disabledContainerColor = Color(0xFFE2E8F0))
                    ) {
                        Text("SELESAI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val formattedPhone = NotificationHelper.formatPhoneNumberForWhatsApp(pickup.nomorHp)
                            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode("Halo Kak, saya kurir SIRAM...")}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("CHAT WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(pickup.alamatLengkap)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(pickup.alamatLengkap)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueProcess)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BUKA MAPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onUpdateStatus(pickup, "DITOLAK") },
                        enabled = !pickup.status.equals("DITOLAK", ignoreCase = true),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger, disabledContainerColor = Color(0xFFE2E8F0))
                    ) {
                        Text("TOLAK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun KalkulatorLapanganDialog(
    pickup: PickupTransaction,
    onDismiss: () -> Unit,
    onConfirmSelesai: (beratAktual: Double, hargaPerKg: Double, totalSaldo: Double, fotoUri: String) -> Unit
) {
    var beratAktualText by remember { mutableStateOf(if (pickup.beratAktualKg > 0) pickup.beratAktualKg.toString() else pickup.estimasiBeratKg.toString()) }
    var hargaPerKgText by remember { mutableStateOf(if (pickup.hargaPerKgAktual > 0) pickup.hargaPerKgAktual.toInt().toString() else "5000") }
    var fotoBuktiUri by remember { mutableStateOf(pickup.fotoBuktiUri) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { fotoBuktiUri = it.toString() }
    }

    val beratDouble = beratAktualText.toDoubleOrNull() ?: 0.0
    val hargaDouble = hargaPerKgText.toDoubleOrNull() ?: 0.0
    val totalEstimasi = beratDouble * hargaDouble

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚖️ Kalkulator Timbangan Lapangan", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Verifikasi berat, harga riil, dan unggah foto bukti penimbangan:", fontSize = 13.sp, color = Color(0xFF64748B))

                OutlinedTextField(
                    value = beratAktualText,
                    onValueChange = { beratAktualText = it },
                    label = { Text("Berat Timbangan Asli (Kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hargaPerKgText,
                    onValueChange = { hargaPerKgText = it },
                    label = { Text("Harga Beli per Kg (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (fotoBuktiUri.isBlank()) "Ambil / Unggah Foto Bukti" else "✅ Foto Bukti Terlampir")
                }

                Surface(
                    color = GreenSuccessBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Saldo Dicairkan Ke Nasabah:", fontSize = 11.sp, color = GreenSuccess)
                        Text(
                            text = currencyFormat.format(totalEstimasi),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenSuccess
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (beratDouble > 0 && hargaDouble > 0) onConfirmSelesai(beratDouble, hargaDouble, totalEstimasi, fotoBuktiUri) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
            ) {
                Text("VERIFIKASI & SELESAI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("BATAL") }
        }
    )
}

private fun startKurirLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    auth: FirebaseAuth,
    fleetRepo: FleetRepository,
    coroutineScope: CoroutineScope
) {
    try {
        val uid = auth.currentUser?.uid ?: "kurir"
        val name = auth.currentUser?.displayName ?: "Kurir Lapangan"

        // 1. Dapatkan lokasi instant terakhir dari cache
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                coroutineScope.launch {
                    fleetRepo.updateLocation(uid, name, loc.latitude, loc.longitude)
                }
            }
        }

        // 2. Dapatkan lokasi terkini presisi tinggi
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                coroutineScope.launch {
                    fleetRepo.updateLocation(uid, name, loc.latitude, loc.longitude)
                }
            }
        }

        // 3. Listener update periodik
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    coroutineScope.launch {
                        fleetRepo.updateLocation(uid, name, location.latitude, location.longitude)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    } catch (e: SecurityException) {
        Log.e("SIRAM_GPS", "Gagal mengaktifkan siaran GPS kurir", e)
    }
}
