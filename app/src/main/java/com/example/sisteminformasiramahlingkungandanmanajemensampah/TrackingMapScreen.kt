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
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityRankedItem
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityTier
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PrimaryGreen = Color(0xFF006C4C)
private val CardBackground = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingMapScreen(
    courierIdToTrack: String = "",
    isAdminMode: Boolean = false,
    userPickupLat: Double = 0.0,
    userPickupLng: Double = 0.0,
    userPickupAddress: String = "",
    viewModel: SampahViewModel? = null,
    onOpenSpkDashboard: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val fleetRepo = remember { FleetRepository() }

    var courierLat by remember { mutableStateOf(-7.4029) } // Default Ngawi
    var courierLng by remember { mutableStateOf(111.4452) }
    var courierName by remember { mutableStateOf("Kurir SIRAM") }
    var isTrackingActive by remember { mutableStateOf(isAdminMode) }
    var selectedCourierInfo by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedPickupRankedItem by remember { mutableStateOf<PriorityRankedItem?>(null) }
    var showPriorityLayers by remember { mutableStateOf(true) }

    // DATA PENJEMPUTAN AKTIF DARI VIEWMODEL / REPOSITORY
    val daftarPickupAdmin = if (viewModel != null) {
        viewModel.semuaPenjemputanAdmin.collectAsState(initial = emptyList()).value
    } else emptyList()

    val criteriaWeights = if (viewModel != null) {
        viewModel.criteriaWeights.collectAsState().value
    } else com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityCriteriaWeights()

    val activePickups = remember(daftarPickupAdmin) {
        daftarPickupAdmin.filter {
            !it.status.equals("SELESAI", ignoreCase = true) &&
                    it.latitude != 0.0 && it.longitude != 0.0
        }
    }

    // HITUNG SKOR PRIORITAS AWCP REAL-TIME BERDASARKAN POSISI GPS KURIR
    // Rolling re-scoring O(n²): dihitung di luar main thread karena diulang tiap pembaruan GPS
    val rankedPickups by produceState(emptyList<PriorityRankedItem>(), activePickups, courierLat, courierLng, criteriaWeights) {
        value = withContext(Dispatchers.Default) {
            AdaptivePriorityEngine.rankPickups(
                pickups = activePickups,
                fleetLat = courierLat,
                fleetLng = courierLng,
                weights = criteriaWeights
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        val initialLat = if (userPickupLat != 0.0) userPickupLat else courierLat
        val initialLng = if (userPickupLng != 0.0) userPickupLng else courierLng
        position = CameraPosition.fromLatLngZoom(LatLng(initialLat, initialLng), 15f)
    }

    // Animasi kamera saat pertama kali atau kurir berpindah signifikan
    LaunchedEffect(courierLat, courierLng) {
        if (userPickupLat == 0.0) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLng(LatLng(courierLat, courierLng)),
                durationMs = 800
            )
        }
    }

    // Listener posisi kurir dari Firestore
    DisposableEffect(isAdminMode, courierIdToTrack) {
        val listener = if (courierIdToTrack == "all") {
            null
        } else if (!isAdminMode) {
            if (courierIdToTrack.isNotBlank()) {
                firestore.collection("fleets").document(courierIdToTrack)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null && snapshot.exists()) {
                            courierLat = snapshot.getDouble("latitude") ?: -7.4029
                            courierLng = snapshot.getDouble("longitude") ?: 111.4452
                            courierName = snapshot.getString("namaKurir") ?: "Kurir SIRAM"
                        }
                    }
            } else {
                firestore.collection("fleets").limit(1)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            val doc = snapshot.documents[0]
                            courierLat = doc.getDouble("latitude") ?: -7.4029
                            courierLng = doc.getDouble("longitude") ?: 111.4452
                            courierName = doc.getString("namaKurir") ?: "Kurir SIRAM"
                        }
                    }
            }
        } else {
            val targetId = auth.currentUser?.uid ?: ""
            if (targetId.isNotBlank()) {
                firestore.collection("fleets").document(targetId)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null && snapshot.exists()) {
                            courierLat = snapshot.getDouble("latitude") ?: -7.4029
                            courierLng = snapshot.getDouble("longitude") ?: 111.4452
                            courierName = snapshot.getString("namaKurir") ?: "Kurir SIRAM"
                        }
                    }
            } else null
        }
        onDispose { listener?.remove() }
    }

    // Multi-courier state for "all"
    var allCouriers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    DisposableEffect(courierIdToTrack) {
        val listener = if (courierIdToTrack == "all") {
            firestore.collection("fleets")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        allCouriers = snapshot.documents.mapNotNull { it.data }.filter { doc ->
                            val lat = (doc["latitude"] as? Number)?.toDouble() ?: 0.0
                            val lng = (doc["longitude"] as? Number)?.toDouble() ?: 0.0
                            !(Math.abs(lat - (-7.2575)) < 0.001 && Math.abs(lng - 112.7521) < 0.001)
                        }
                    }
                }
        } else null
        onDispose { listener?.remove() }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        if (isAdminMode || courierIdToTrack.isBlank() || courierIdToTrack == "all") {
                            courierLat = loc.latitude
                            courierLng = loc.longitude
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("SIRAM_GPS", "Gagal mengambil lokasi awal", e)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            isTrackingActive = true
        }
    }

    LaunchedEffect(isTrackingActive) {
        if (isAdminMode && isTrackingActive) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                return@LaunchedEffect
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1000)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        courierLat = location.latitude
                        courierLng = location.longitude
                        coroutineScope.launch {
                            val uid = auth.currentUser?.uid ?: "admin_kurir"
                            val name = auth.currentUser?.displayName ?: "Kurir Lapangan"
                            fleetRepo.updateLocation(uid, name, location.latitude, location.longitude)
                        }
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e("SIRAM_GPS", "Security exception", e)
            }
        }
    }

    // Animasi Pulse untuk Marker & Badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isAdminMode) "Navigasi Prioritas Armada" else "Lacak Posisi Kurir",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Adaptive Priority Routing System",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSpkDashboard) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Buka Dashboard SPK",
                            tint = PrimaryGreen
                        )
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
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // 1. TITIK JEMPUT TUNGGAL NASABAH (JIKA MODE NASABAH)
                if (userPickupLat != 0.0 && userPickupLng != 0.0) {
                    val pickupMarkerState = rememberMarkerState(key = "user_dest", position = LatLng(userPickupLat, userPickupLng))
                    MarkerComposable(
                        state = pickupMarkerState,
                        title = "📍 Titik Penjemputan Anda",
                        snippet = userPickupAddress.ifBlank { "Lokasi Penjemputan Sampah" },
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = Color(0xFF15803D),
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = "📍 Lokasi Jemput Anda",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // 2. TITIK-TITIK PENJEMPUTAN SPK DENGAN URUTAN PRIORITAS (#1, #2, dst.)
                if (showPriorityLayers && rankedPickups.isNotEmpty()) {
                    rankedPickups.forEach { rankedItem ->
                        val p = rankedItem.pickup
                        val b = rankedItem.breakdown
                        val pos = LatLng(p.latitude, p.longitude)
                        val markerState = rememberMarkerState(key = "priority_pin_${p.id}", position = pos)

                        val pinBgColor = when (b.tier) {
                            PriorityTier.CRITICAL -> Color(0xFFDC2626)
                            PriorityTier.HIGH -> Color(0xFFEA580C)
                            PriorityTier.MEDIUM -> Color(0xFFCA8A04)
                            PriorityTier.NORMAL -> Color(0xFF16A34A)
                        }

                        MarkerComposable(
                            state = markerState,
                            title = "Prioritas #${rankedItem.rank} (Skor ${String.format("%.1f", b.totalScore)})",
                            snippet = "${p.alamatLengkap} • ${p.jenisSampah}",
                            onClick = {
                                selectedPickupRankedItem = rankedItem
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(pos, 16f),
                                        durationMs = 600
                                    )
                                }
                                true
                            },
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    color = pinBgColor,
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 6.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "#${rankedItem.rank}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "${String.format("%.0f", b.totalScore)}★",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.PinDrop,
                                    contentDescription = null,
                                    tint = pinBgColor,
                                    modifier = Modifier.size(if (rankedItem.rank == 1) 36.dp else 30.dp)
                                )
                            }
                        }
                    }
                }

                // 3. MARKER ARMADA / KURIR
                if (courierIdToTrack == "all") {
                    allCouriers.forEach { courier ->
                        val lat = (courier["latitude"] as? Number)?.toDouble() ?: -7.4029
                        val lng = (courier["longitude"] as? Number)?.toDouble() ?: 111.4452
                        if (lat != -7.2575 && lng != 112.7521) {
                            val name = courier["namaKurir"] as? String ?: "Kurir Lapangan"
                            val courierState = rememberMarkerState(key = "courier_${courier["courierId"] ?: name}", position = LatLng(lat, lng))
                            MarkerComposable(
                                state = courierState,
                                title = name,
                                snippet = "Klik untuk rincian armada",
                                onClick = {
                                    selectedCourierInfo = courier
                                    true
                                },
                                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((36 * pulseScale).dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B82F6).copy(alpha = pulseAlpha * 0.6f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1D4ED8)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val singleMarkerState = rememberMarkerState(key = "single_courier", position = LatLng(courierLat, courierLng))
                    LaunchedEffect(courierLat, courierLng) {
                        singleMarkerState.position = LatLng(courierLat, courierLng)
                    }
                    MarkerComposable(
                        state = singleMarkerState,
                        title = courierName,
                        snippet = if (isAdminMode) "Posisi Anda (Live Broadcasting)" else "Kurir Sedang Menuju Lokasi Anda",
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((36 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6).copy(alpha = pulseAlpha * 0.6f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1D4ED8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // INDIKATOR LIVE LOCATION BADGE ATAS
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(50.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = pulseAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1D4ED8))
                        )
                    }
                    Text(
                        text = if (isAdminMode) "🟢 Armada Online: Siar GPS" else "🔵 Live Location: $courierName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF0F172A)
                    )

                    if (rankedPickups.isNotEmpty()) {
                        Text("•", color = Color(0xFFCBD5E1))
                        Text(
                            text = "${rankedPickups.size} Titik SPK",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            // FLOATING CARD 1: DETAIL TITIK PENJEMPUTAN YANG DIPILIH DARI PIN
            selectedPickupRankedItem?.let { item ->
                val p = item.pickup
                val b = item.breakdown

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = Color(b.tier.badgeColorHex),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("#${item.rank}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Text(
                                    text = b.tier.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(b.tier.badgeColorHex)
                                )
                            }
                            IconButton(onClick = { selectedPickupRankedItem = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup")
                            }
                        }

                        Text(
                            text = p.alamatLengkap,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = "Kategori: ${p.jenisSampah} • Kepenuhan: ${p.tingkatKepenuhan}% • Urgensi: ${p.urgensiWarga}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // PENALARAN SPK
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🤖 SPK Reasoning: ${b.reasoningSummary}",
                                fontSize = 11.sp,
                                color = Color(0xFF334155),
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val gmmUri = Uri.parse("google.navigation:q=${p.latitude},${p.longitude}&mode=d")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${p.latitude},${p.longitude}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("NAVIGASI RUTE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (p.nomorHp.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val formatted = NotificationHelper.formatPhoneNumberForWhatsApp(p.nomorHp)
                                        val url = "https://api.whatsapp.com/send?phone=$formatted&text=${Uri.encode("Halo, kami armada SIRAM sedang menuju lokasi penjemputan sampah Anda di ${p.alamatLengkap}.")}"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("HUBUNGI WARGA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // FLOATING CARD 2: REKOMENDASI TITIK UTAMA (#1) JIKA BELUM ADA PIN YANG DIPILIH
            if (selectedPickupRankedItem == null && selectedCourierInfo == null && rankedPickups.isNotEmpty()) {
                val topPick = rankedPickups[0]
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = Color(0xFFDC2626),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("#1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                                Text(
                                    text = "Rekomendasi Rute Prioritas SPK",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "AWCP ${String.format("%.1f", topPick.breakdown.totalScore)}★",
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = topPick.pickup.alamatLengkap,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "💡 ${topPick.breakdown.reasoningSummary}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenSpkDashboard,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Analisis SPK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    selectedPickupRankedItem = topPick
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(topPick.pickup.latitude, topPick.pickup.longitude),
                                                16f
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Fokus Titik Ini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // POPUP RINCIAN KURIR KETIKA KURIR DIKLIK
            selectedCourierInfo?.let { courier ->
                val name = courier["namaKurir"] as? String ?: "Kurir Lapangan"
                val lat = (courier["latitude"] as? Number)?.toDouble() ?: -7.4029
                val lng = (courier["longitude"] as? Number)?.toDouble() ?: 111.4452
                val phone = courier["nomorHp"] as? String ?: ""

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = Color(0xFFE0F2FE)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0284C7))
                                    }
                                }
                                Column {
                                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = "🟢 Status: Aktif Siar Live GPS", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(onClick = { selectedCourierInfo = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = String.format("📍 Posisi GPS: %.5f, %.5f", lat, lng),
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            if (phone.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val formatted = NotificationHelper.formatPhoneNumberForWhatsApp(phone)
                                        val url = "https://api.whatsapp.com/send?phone=$formatted&text=${Uri.encode("Halo $name, saya Admin SIRAM...")}"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Text("CHAT WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    val gmmUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($name)")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, gmmUri))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("BUKA MAPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
