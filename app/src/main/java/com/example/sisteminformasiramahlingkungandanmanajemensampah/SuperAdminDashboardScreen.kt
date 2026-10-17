package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PenarikanSaldo
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah
import com.google.firebase.firestore.FirebaseFirestore
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
fun SuperAdminDashboardScreen(
    viewModel: SampahViewModel,
    onPriceManagerClick: () -> Unit = {},
    onTrackingClick: () -> Unit = {},
    onBroadcastClick: () -> Unit = {},
    onSpkPriorityClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    var activeModule by remember { mutableStateOf("home") }

    BackHandler(enabled = activeModule != "home") {
        activeModule = "home"
    }

    val auth = remember { FirebaseAuth.getInstance() }
    val fleetRepo = remember { FleetRepository() }
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            startSuperAdminLocationUpdates(fusedLocationClient, auth, fleetRepo, coroutineScope)
        }
    }

    LaunchedEffect(Unit) {
        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocation) {
            startSuperAdminLocationUpdates(fusedLocationClient, auth, fleetRepo, coroutineScope)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var selectedFilterPickup by remember { mutableStateOf("MENUNGGU") }
    var selectedFilterSampah by remember { mutableStateOf("MENUNGGU") }
    var selectedFilterPenarikan by remember { mutableStateOf("MENUNGGU") }
    var searchQuery by remember { mutableStateOf("") }

    val daftarPickupAdmin by viewModel.semuaPenjemputanAdmin.collectAsState(initial = emptyList())
    val daftarSampahAdmin by viewModel.semuaSampahAdmin.collectAsState(initial = emptyList())
    val daftarPenarikanAdmin by viewModel.semuaPenarikanAdmin.collectAsState(initial = emptyList())

    var showSelesaiDialog by remember { mutableStateOf(false) }
    var selectedPickupForSelesai by remember { mutableStateOf<PickupTransaction?>(null) }
    var showPilihKurirDialog by remember { mutableStateOf(false) }
    var selectedPickupForKurir by remember { mutableStateOf<PickupTransaction?>(null) }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }
    var showCatatSampahDialog by remember { mutableStateOf(false) }
    var selectedSampahForSetuju by remember { mutableStateOf<Sampah?>(null) }
    var showPencairanDialog by remember { mutableStateOf(false) }
    var selectedPenarikanForSukses by remember { mutableStateOf<PenarikanSaldo?>(null) }
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var adminMessage by remember { mutableStateOf("") }
    var staffList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            firestore.collection("users").get()
                .addOnSuccessListener { result ->
                    staffList = result.documents.map { doc -> doc.data ?: emptyMap() }
                }
                .addOnFailureListener { e ->
                    Log.e("SIRAM_SUPER", "Gagal memuat staff", e)
                }
        } catch (e: Exception) {
            Log.e("SIRAM_SUPER", "Exception staff", e)
        }
    }

    LaunchedEffect(showAddAdminDialog || showPilihKurirDialog) {
        if (showAddAdminDialog || showPilihKurirDialog) {
            try {
                firestore.collection("users").get()
                    .addOnSuccessListener { result ->
                        staffList = result.documents.map { doc -> doc.data ?: emptyMap() }
                    }
                    .addOnFailureListener { e ->
                        Log.e("SIRAM_SUPER", "Gagal memuat staff dialog", e)
                    }
            } catch (e: Exception) {
                Log.e("SIRAM_SUPER", "Exception dialog", e)
            }
        }
    }

    val listStatusPickup = listOf("MENUNGGU", "DIPROSES", "MENUNGGU_VERIFIKASI", "SELESAI", "DITOLAK", "SEMUA")
    val listStatusSampah = listOf("MENUNGGU", "DISETUJUI", "DITOLAK", "SEMUA")
    val listStatusPenarikan = listOf("MENUNGGU", "SUKSES", "DITOLAK", "SEMUA")
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val totalTonaseSelesai = remember(daftarPickupAdmin) {
        daftarPickupAdmin.filter { it.status.equals("SELESAI", ignoreCase = true) }
            .sumOf { if (it.beratAktualKg > 0) it.beratAktualKg else it.estimasiBeratKg }
    }

    val totalNominalSelesai = remember(daftarPickupAdmin) {
        daftarPickupAdmin.filter { it.status.equals("SELESAI", ignoreCase = true) }
            .sumOf { it.totalNominalRp }
    }

    val filteredPickupList = remember(daftarPickupAdmin, selectedFilterPickup, searchQuery) {
        daftarPickupAdmin.filter { pickup ->
            val matchesTab = when (selectedFilterPickup) {
                "MENUNGGU" -> pickup.status.equals("MENUNGGU", ignoreCase = true)
                "DIPROSES" -> pickup.status.equals("DIPROSES", ignoreCase = true)
                "MENUNGGU_VERIFIKASI" -> pickup.status.equals("MENUNGGU_VERIFIKASI", ignoreCase = true)
                "SELESAI" -> pickup.status.equals("SELESAI", ignoreCase = true)
                "DITOLAK" -> pickup.status.equals("DITOLAK", ignoreCase = true)
                else -> true
            }
            val matchesSearch = pickup.jenisSampah.contains(searchQuery, ignoreCase = true) ||
                    pickup.alamatLengkap.contains(searchQuery, ignoreCase = true) ||
                    pickup.nomorHp.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
        }
    }

    val filteredSampahList = remember(daftarSampahAdmin, selectedFilterSampah, searchQuery) {
        daftarSampahAdmin.filter { sampah ->
            val matchesTab = when (selectedFilterSampah) {
                "MENUNGGU" -> sampah.status.equals("MENUNGGU", ignoreCase = true)
                "DISETUJUI" -> sampah.status.equals("DISETUJUI", ignoreCase = true)
                "DITOLAK" -> sampah.status.equals("DITOLAK", ignoreCase = true)
                else -> true
            }
            val matchesSearch = sampah.jenisSampah.contains(searchQuery, ignoreCase = true) ||
                    sampah.catatan.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
        }
    }

    val filteredPenarikanList = remember(daftarPenarikanAdmin, selectedFilterPenarikan, searchQuery) {
        daftarPenarikanAdmin.filter { p ->
            val matchesTab = when (selectedFilterPenarikan) {
                "MENUNGGU" -> p.status.equals("MENUNGGU", ignoreCase = true)
                "SUKSES" -> p.status.equals("SUKSES", ignoreCase = true)
                "DITOLAK" -> p.status.equals("DITOLAK", ignoreCase = true)
                else -> true
            }
            val matchesSearch = p.metodePenarikan.contains(searchQuery, ignoreCase = true) ||
                    p.nomorTujuan.contains(searchQuery, ignoreCase = true) ||
                    p.namaPemilik.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (activeModule != "home") {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (activeModule) {
                                    "jemput" -> "Validasi Penjemputan Sampah"
                                    "catat" -> "Validasi Setor Mandiri"
                                    "pencairan" -> "Validasi Pencairan Saldo"
                                    "statistik" -> "Statistik & Analitik Sistem"
                                    "tim" -> "Manajemen Tim & Kurir"
                                    else -> "Super Admin Module"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { activeModule = "home" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBackground)
                    )
                } else {
                    // HEADER BANNER SUPER ADMIN (Kompak seperti sebelumnya)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF0F172A), PrimaryGreen)
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(
                                        text = "🔒 Super Admin",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Enterprise Command Center",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Pusat Kendali Operasional & Analitik Terpusat",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            IconButton(
                                onClick = onLogoutClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (activeModule == "home") {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Total Tonase Daur Ulang", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                                        Text(
                                            text = String.format("%.1f Kg", totalTonaseSelesai),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }
                                    VerticalDivider(modifier = Modifier.height(36.dp), color = Color(0xFFE2E8F0))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Total Saldo Tervalidasi", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                                        Text(
                                            text = currencyFormat.format(totalNominalSelesai),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BlueProcess
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SuperAdminControlCenterGrid(
                                onJemputClick = { activeModule = "jemput" },
                                onCatatClick = { activeModule = "catat" },
                                onPencairanClick = { activeModule = "pencairan" },
                                onStatistikClick = { activeModule = "statistik" },
                                onTimClick = { activeModule = "tim" },
                                onTrackingClick = onTrackingClick,
                                onPriceClick = onPriceManagerClick,
                                onBroadcastClick = onBroadcastClick,
                                onAddKurirClick = { showAddAdminDialog = true },
                                onSpkPriorityClick = onSpkPriorityClick
                            )
                        }
                    }
                } else {
                    val targetModule = when (activeModule) {
                        "jemput" -> 0
                        "catat" -> 1
                        "pencairan" -> 2
                        else -> 0
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari data...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )

                    if (activeModule == "statistik") {
                        SuperAdminStatisticsView(
                            totalTonase = totalTonaseSelesai,
                            totalNominal = totalNominalSelesai,
                            totalKurir = staffList.size,
                            totalPickup = daftarPickupAdmin.size,
                            totalSampah = daftarSampahAdmin.size
                        )
                    } else if (activeModule == "tim") {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(staffList) { staff ->
                                StaffMemberCard(staff = staff)
                            }
                        }
                    } else {
                        if (targetModule == 2) {
                            Button(
                                onClick = {
                                    val totalTonase = daftarPickupAdmin.filter { it.status.equals("SELESAI", ignoreCase = true) }.sumOf { if (it.beratAktualKg > 0) it.beratAktualKg else it.estimasiBeratKg }
                                    val totalNominal = daftarPenarikanAdmin.filter { it.status.equals("SUKSES", ignoreCase = true) }.sumOf { it.nominalRp }
                                    PdfGeneratorHelper.cetakLaporanKeuanganPdf(context, totalTonase, totalNominal, staffList.size)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CETAK LAPORAN KEUANGAN PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        when (targetModule) {
                            0 -> {
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

                                AnimatedVisibility(visible = true) {
                                    if (filteredPickupList.isEmpty()) {
                                        EmptyStateView(message = "Tidak ada penjemputan di tab ini.")
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
                                                        if (statusBaru == "DIPROSES") {
                                                            selectedPickupForKurir = item
                                                            showPilihKurirDialog = true
                                                        } else if (statusBaru == "SELESAI") {
                                                            selectedPickupForSelesai = item
                                                            showSelesaiDialog = true
                                                        } else {
                                                            viewModel.updateStatusPenjemputanByAdmin(item, statusBaru)
                                                            selectedFilterPickup = statusBaru
                                                        }
                                                    },
                                                    onViewPhoto = { uri ->
                                                        previewPhotoUri = uri
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                ScrollableTabRow(
                                    selectedTabIndex = listStatusSampah.indexOf(selectedFilterSampah),
                                    edgePadding = 20.dp,
                                    divider = {},
                                    containerColor = Color.Transparent,
                                    contentColor = PrimaryGreen,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listStatusSampah.forEach { filter ->
                                        Tab(
                                            selected = selectedFilterSampah == filter,
                                            onClick = { selectedFilterSampah = filter },
                                            text = { Text(filter, fontWeight = FontWeight.Medium, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = true) {
                                    if (filteredSampahList.isEmpty()) {
                                        EmptyStateView(message = "Tidak ada catat sampah mandiri.")
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredSampahList, key = { it.id }) { sampah ->
                                                CommercialCatatSampahCard(
                                                    sampah = sampah,
                                                    onValidasi = { item, statusBaru ->
                                                        if (statusBaru == "DISETUJUI") {
                                                            selectedSampahForSetuju = item
                                                            showCatatSampahDialog = true
                                                        } else {
                                                            viewModel.updateStatusSampahMandiriByAdmin(item, statusBaru)
                                                            selectedFilterSampah = statusBaru
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                ScrollableTabRow(
                                    selectedTabIndex = listStatusPenarikan.indexOf(selectedFilterPenarikan),
                                    edgePadding = 20.dp,
                                    divider = {},
                                    containerColor = Color.Transparent,
                                    contentColor = PrimaryGreen,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listStatusPenarikan.forEach { filter ->
                                        Tab(
                                            selected = selectedFilterPenarikan == filter,
                                            onClick = { selectedFilterPenarikan = filter },
                                            text = { Text(filter, fontWeight = FontWeight.Medium, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = true) {
                                    if (filteredPenarikanList.isEmpty()) {
                                        EmptyStateView(message = "Tidak ada pengajuan pencairan.")
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredPenarikanList, key = { it.id }) { penarikan ->
                                                CommercialPenarikanSaldoCard(
                                                    penarikan = penarikan,
                                                    onValidasi = { item, statusBaru ->
                                                        if (statusBaru == "SUKSES") {
                                                            selectedPenarikanForSukses = item
                                                            showPencairanDialog = true
                                                        } else {
                                                            viewModel.updateStatusPenarikanByAdmin(item, statusBaru)
                                                            selectedFilterPenarikan = statusBaru
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
                }
            }
        }
    }

    // Dialogs
    if (showSelesaiDialog && selectedPickupForSelesai != null) {
        KalkulatorLapanganDialog(
            pickup = selectedPickupForSelesai!!,
            onDismiss = { showSelesaiDialog = false; selectedPickupForSelesai = null },
            onConfirmSelesai = { beratAktual, hargaPerKg, totalSaldo ->
                val pickupUpdated = selectedPickupForSelesai!!.copy(
                    beratAktualKg = beratAktual,
                    hargaPerKgAktual = hargaPerKg,
                    totalNominalRp = totalSaldo,
                    status = "SELESAI"
                )
                viewModel.updateStatusPenjemputanByAdmin(pickupUpdated, "SELESAI")
                showSelesaiDialog = false
                selectedPickupForSelesai = null
            }
        )
    }

    if (showPilihKurirDialog && selectedPickupForKurir != null) {
        PilihKurirDialog(
            staffList = staffList,
            onDismiss = {
                showPilihKurirDialog = false
                selectedPickupForKurir = null
            },
            onKurirSelected = { kurirId, kurirName ->
                viewModel.assignKurirPenjemputan(selectedPickupForKurir!!, kurirId, kurirName)
                showPilihKurirDialog = false
                selectedPickupForKurir = null
            }
        )
    }

    if (previewPhotoUri != null) {
        PhotoPreviewDialog(
            photoUri = previewPhotoUri!!,
            onDismiss = { previewPhotoUri = null }
        )
    }

    if (showCatatSampahDialog && selectedSampahForSetuju != null) {
        KalkulatorCatatSampahDialog(
            sampah = selectedSampahForSetuju!!,
            onDismiss = { showCatatSampahDialog = false; selectedSampahForSetuju = null },
            onConfirmSetuju = { beratBaru, hargaBaru, totalBaru ->
                viewModel.updateStatusSampahMandiriWithDetailsByAdmin(
                    sampah = selectedSampahForSetuju!!,
                    beratBaru = beratBaru,
                    hargaBaru = hargaBaru,
                    totalBaru = totalBaru,
                    statusBaru = "DISETUJUI"
                )
                showCatatSampahDialog = false
                selectedSampahForSetuju = null
            }
        )
    }

    if (showPencairanDialog && selectedPenarikanForSukses != null) {
        ValidasiPencairanDialog(
            penarikan = selectedPenarikanForSukses!!,
            onDismiss = { showPencairanDialog = false; selectedPenarikanForSukses = null },
            onConfirmSukses = { nominalBaru ->
                viewModel.updateStatusPenarikanWithDetailsByAdmin(
                    penarikan = selectedPenarikanForSukses!!,
                    nominalBaru = nominalBaru,
                    statusBaru = "SUKSES"
                )
                showPencairanDialog = false
                selectedPenarikanForSukses = null
            }
        )
    }

    if (showAddAdminDialog) {
        TambahAdminDialog(
            onDismiss = { showAddAdminDialog = false },
            onRegister = { email, pass, nama, role ->
                viewModel.registerAdminBaru(email, pass, nama, role) { success, msg ->
                    adminMessage = msg
                    if (success) showAddAdminDialog = false
                }
            }
        )
    }

    if (adminMessage.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { adminMessage = "" },
            title = { Text("Informasi", fontWeight = FontWeight.Bold) },
            text = { Text(adminMessage) },
            confirmButton = {
                Button(onClick = { adminMessage = "" }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun PhotoPreviewDialog(
    photoUri: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foto Bukti Penimbangan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Bukti Penimbangan",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("TUTUP")
            }
        }
    )
}

@Composable
private fun PilihKurirDialog(
    staffList: List<Map<String, Any>>,
    onDismiss: () -> Unit,
    onKurirSelected: (kurirId: String, kurirName: String) -> Unit
) {
    val kurirOnly = staffList.filter { (it["role"] as? String)?.lowercase() == "admin" || (it["role"] as? String)?.lowercase() == "super_admin" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Kurir Penjemputan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pilih petugas kurir lapangan yang akan menangani penjemputan ini:", fontSize = 12.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                if (kurirOnly.isEmpty()) {
                    Text("Belum ada kurir terdaftar. Tambahkan kurir di menu 'Tambah Kurir'.", fontSize = 12.sp, color = RedDanger)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 250.dp)) {
                        items(kurirOnly) { kurir ->
                            val uid = kurir["uid"] as? String ?: ""
                            val nama = kurir["nama"] as? String ?: "Kurir Lapangan"
                            val role = kurir["role"] as? String ?: "admin"

                            Surface(
                                onClick = { onKurirSelected(uid, nama) },
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = nama, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                        Text(text = "Peran: ${role.uppercase()}", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("BATAL") }
        }
    )
}

@Composable
private fun SuperAdminControlCenterGrid(
    onJemputClick: () -> Unit,
    onCatatClick: () -> Unit,
    onPencairanClick: () -> Unit,
    onStatistikClick: () -> Unit,
    onTimClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onPriceClick: () -> Unit,
    onBroadcastClick: () -> Unit,
    onAddKurirClick: () -> Unit,
    onSpkPriorityClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Enterprise Command Center",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Pusat Kendali Operasional Terpusat",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = "10 Modul (DSS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.5.dp)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalShipping,
                        label = "Jemput",
                        color = Color(0xFF0284C7),
                        onClick = onJemputClick
                    )
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AssignmentTurnedIn,
                        label = "Setor Mandiri",
                        color = PrimaryGreen,
                        onClick = onCatatClick
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Pencairan",
                        color = Color(0xFFD97706),
                        onClick = onPencairanClick
                    )
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Analytics,
                        label = "Statistik",
                        color = Color(0xFF8B5CF6),
                        onClick = onStatistikClick
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Group,
                        label = "Tim Kurir",
                        color = Color(0xFF0EA5E9),
                        onClick = onTimClick
                    )
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PersonAdd,
                        label = "Kurir Baru",
                        color = Color(0xFF10B981),
                        onClick = onAddKurirClick
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MyLocation,
                        label = "Live GPS",
                        color = Color(0xFF6366F1),
                        onClick = onTrackingClick
                    )
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalOffer,
                        label = "Tarif Harga",
                        color = Color(0xFFEC4899),
                        onClick = onPriceClick
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bolt,
                        label = "SPK Prioritas (DSS)",
                        color = Color(0xFFDC2626),
                        onClick = onSpkPriorityClick
                    )
                    AdminCommandTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Campaign,
                        label = "Broadcast Info",
                        color = Color(0xFFF59E0B),
                        onClick = onBroadcastClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SuperAdminStatisticsView(
    totalTonase: Double,
    totalNominal: Double,
    totalKurir: Int,
    totalPickup: Int,
    totalSampah: Int
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column {
                Text("Statistik & Analitik Sistem", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                Text("Ringkasan data operasional bank sampah secara keseluruhan", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Tonase", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = String.format("%.1f Kg", totalTonase), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Saldo Nasabah", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = currencyFormat.format(totalNominal), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Kurir / Staff", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$totalKurir Orang", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Transaksi Masuk", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${totalPickup + totalSampah} Item", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCommandTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun KalkulatorLapanganDialog(
    pickup: PickupTransaction,
    onDismiss: () -> Unit,
    onConfirmSelesai: (beratAktual: Double, hargaPerKg: Double, totalSaldo: Double) -> Unit
) {
    var beratAktualText by remember { mutableStateOf(if (pickup.beratAktualKg > 0) pickup.beratAktualKg.toString() else pickup.estimasiBeratKg.toString()) }
    var hargaPerKgText by remember { mutableStateOf(if (pickup.hargaPerKgAktual > 0) pickup.hargaPerKgAktual.toInt().toString() else "5000") }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val beratDouble = beratAktualText.toDoubleOrNull() ?: 0.0
    val hargaDouble = hargaPerKgText.toDoubleOrNull() ?: 0.0
    val totalEstimasi = beratDouble * hargaDouble

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚖️ Kalkulator Timbangan Lapangan", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Verifikasi berat dan harga riil untuk jenis sampah: ${pickup.jenisSampah}", fontSize = 13.sp, color = Color(0xFF64748B))

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
                onClick = { if (beratDouble > 0 && hargaDouble > 0) onConfirmSelesai(beratDouble, hargaDouble, totalEstimasi) },
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

@Composable
private fun KalkulatorCatatSampahDialog(
    sampah: Sampah,
    onDismiss: () -> Unit,
    onConfirmSetuju: (beratBaru: Double, hargaBaru: Double, totalBaru: Double) -> Unit
) {
    var beratText by remember { mutableStateOf(sampah.berat.toString()) }
    var hargaText by remember { mutableStateOf(sampah.hargaPerKg.toInt().toString()) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val beratDouble = beratText.toDoubleOrNull() ?: 0.0
    val hargaDouble = hargaText.toDoubleOrNull() ?: 0.0
    val totalEstimasi = beratDouble * hargaDouble

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚖️ Validasi & Koreksi Catat Sampah", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sesuaikan berat atau harga riil untuk jenis sampah: ${sampah.jenisSampah}", fontSize = 13.sp, color = Color(0xFF64748B))

                OutlinedTextField(
                    value = beratText,
                    onValueChange = { beratText = it },
                    label = { Text("Berat Sampah (Kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hargaText,
                    onValueChange = { hargaText = it },
                    label = { Text("Harga Beli per Kg (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = GreenSuccessBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Saldo Masuk Ke Nasabah:", fontSize = 11.sp, color = GreenSuccess)
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
                onClick = { if (beratDouble > 0 && hargaDouble > 0) onConfirmSetuju(beratDouble, hargaDouble, totalEstimasi) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
            ) {
                Text("SETUJUI & MASUKKAN SALDO")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("BATAL") }
        }
    )
}

@Composable
private fun ValidasiPencairanDialog(
    penarikan: PenarikanSaldo,
    onDismiss: () -> Unit,
    onConfirmSukses: (nominalBaru: Double) -> Unit
) {
    var nominalText by remember { mutableStateOf(penarikan.nominalRp.toInt().toString()) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val nominalDouble = nominalText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💸 Validasi & Konfirmasi Pencairan Saldo", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Verifikasi detail rekening/e-wallet nasabah sebelum melakukan transfer:", fontSize = 13.sp, color = Color(0xFF64748B))

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Metode: ${penarikan.metodePenarikan}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Nomor Tujuan: ${penarikan.nomorTujuan}", fontSize = 12.sp, color = Color(0xFF334155))
                        Text("Nama Pemilik: ${penarikan.namaPemilik}", fontSize = 12.sp, color = Color(0xFF334155))
                    }
                }

                OutlinedTextField(
                    value = nominalText,
                    onValueChange = { nominalText = it },
                    label = { Text("Nominal Transfer (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = GreenSuccessBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Transfer Disetujui:", fontSize = 11.sp, color = GreenSuccess)
                        Text(
                            text = currencyFormat.format(nominalDouble),
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
                onClick = { if (nominalDouble > 0) onConfirmSukses(nominalDouble) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
            ) {
                Text("KONFIRMASI TRANSFER SUKSES")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("BATAL") }
        }
    )
}

@Composable
private fun TambahAdminDialog(
    onDismiss: () -> Unit,
    onRegister: (email: String, pass: String, nama: String, role: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("admin") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Akun Admin / Kurir Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Daftarkan akun operasional baru untuk kurir lapangan atau admin penjemputan.", fontSize = 12.sp, color = Color(0xFF64748B))

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Login") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (Min. 6 Karakter)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pilih Peran (Role):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedRole == "admin",
                        onClick = { selectedRole = "admin" },
                        label = { Text("Kurir Lapangan") }
                    )
                    FilterChip(
                        selected = selectedRole == "super_admin",
                        onClick = { selectedRole = "super_admin" },
                        label = { Text("Super Admin") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isNotBlank() && password.length >= 6 && nama.isNotBlank()) {
                        onRegister(email.trim(), password.trim(), nama.trim(), selectedRole)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("DAFTARKAN AKUN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("BATAL") }
        }
    )
}

@Composable
private fun StaffMemberCard(staff: Map<String, Any>) {
    val nama = staff["nama"] as? String ?: "Tanpa Nama"
    val email = staff["email"] as? String ?: "-"
    val role = staff["role"] as? String ?: "admin"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = nama, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                Text(text = email, fontSize = 12.sp, color = Color(0xFF64748B))
            }

            Surface(
                color = if (role == "super_admin") Color(0xFFFEF3C7) else Color(0xFFE0F2FE),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = role.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (role == "super_admin") Color(0xFFD97706) else Color(0xFF0284C7),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CommercialPickupCard(
    pickup: PickupTransaction,
    onUpdateStatus: (item: PickupTransaction, statusBaru: String) -> Unit,
    onViewPhoto: (String) -> Unit
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

            // BADGE KONDISI REAL-TIME & URGENSI
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                Surface(
                    color = if (pickup.tingkatKepenuhan >= 85) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "🗑️ Kepenuhan: ${pickup.tingkatKepenuhan}%",
                        color = if (pickup.tingkatKepenuhan >= 85) Color(0xFFDC2626) else Color(0xFFD97706),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = if (pickup.urgensiWarga.equals("DARURAT", ignoreCase = true)) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "⚠️ ${pickup.urgensiWarga}",
                        color = if (pickup.urgensiWarga.equals("DARURAT", ignoreCase = true)) Color(0xFFDC2626) else Color(0xFF0284C7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "📦 ${pickup.jenisWadah}",
                        color = Color(0xFF475569),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (pickup.assignedKurirName.isNotBlank()) {
                Text(text = "👤 Kurir Ditugaskan: ${pickup.assignedKurirName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
            }

            if (pickup.fotoBuktiUri.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { onViewPhoto(pickup.fotoBuktiUri) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIHAT FOTO BUKTI PENIMBANGAN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (pickup.catatan.isNotBlank()) {
                Text(text = "Catatan: ${pickup.catatan}", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onUpdateStatus(pickup, "DIPROSES") },
                    enabled = pickup.status.uppercase() == "MENUNGGU",
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueProcess, disabledContainerColor = Color(0xFFE2E8F0))
                ) {
                    Text("PROSES", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onUpdateStatus(pickup, "SELESAI") },
                    enabled = pickup.status.uppercase() == "MENUNGGU_VERIFIKASI",
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, disabledContainerColor = Color(0xFFE2E8F0))
                ) {
                    Text("SETUJUI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val formattedPhone = NotificationHelper.formatPhoneNumberForWhatsApp(pickup.nomorHp)
                        val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode("Halo Kak, saya admin SIRAM...")}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("CHAT WA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onUpdateStatus(pickup, "DITOLAK") },
                    enabled = !pickup.status.equals("DITOLAK", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger, disabledContainerColor = Color(0xFFE2E8F0))
                ) {
                    Text("TOLAK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CommercialCatatSampahCard(
    sampah: Sampah,
    onValidasi: (sampah: Sampah, statusBaru: String) -> Unit
) {
    val (statusColor, statusBg) = when (sampah.status.uppercase()) {
        "DISETUJUI" -> GreenSuccess to GreenSuccessBg
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = sampah.jenisSampah, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                Surface(color = statusBg, shape = RoundedCornerShape(50.dp)) {
                    Text(text = sampah.status.uppercase(), fontWeight = FontWeight.Bold, color = statusColor, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Berat: ${sampah.berat} Kg • Total: Rp ${sampah.totalNilai.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onValidasi(sampah, "DISETUJUI") },
                    enabled = !sampah.status.equals("DISETUJUI", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                ) {
                    Text("SETUJUI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onValidasi(sampah, "DITOLAK") },
                    enabled = !sampah.status.equals("DITOLAK", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("TOLAK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CommercialPenarikanSaldoCard(
    penarikan: PenarikanSaldo,
    onValidasi: (item: PenarikanSaldo, statusBaru: String) -> Unit
) {
    val (statusColor, statusBg) = when (penarikan.status.uppercase()) {
        "SUKSES" -> GreenSuccess to GreenSuccessBg
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = penarikan.metodePenarikan, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                Surface(color = statusBg, shape = RoundedCornerShape(50.dp)) {
                    Text(text = penarikan.status.uppercase(), fontWeight = FontWeight.Bold, color = statusColor, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Tujuan: ${penarikan.nomorTujuan} (${penarikan.namaPemilik})", fontSize = 13.sp, color = Color(0xFF334155))
            Text(text = "Nominal: Rp ${penarikan.nominalRp.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onValidasi(penarikan, "SUKSES") },
                    enabled = !penarikan.status.equals("SUKSES", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                ) {
                    Text("TRANSFER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onValidasi(penarikan, "DITOLAK") },
                    enabled = !penarikan.status.equals("DITOLAK", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("TOLAK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun startSuperAdminLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    auth: FirebaseAuth,
    fleetRepo: FleetRepository,
    coroutineScope: CoroutineScope
) {
    try {
        val uid = auth.currentUser?.uid ?: "super_admin"
        val name = auth.currentUser?.displayName?.ifBlank { "Super Admin (Anda)" } ?: "Super Admin (Anda)"

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                coroutineScope.launch {
                    fleetRepo.updateLocation(uid, name, loc.latitude, loc.longitude)
                }
            }
        }

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
        Log.e("SIRAM_GPS", "Gagal mengaktifkan siaran GPS Super Admin", e)
    }
}
