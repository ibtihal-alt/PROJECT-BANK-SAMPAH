package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

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

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(courierLat, courierLng), 16f)
    }

    // Animasi kamera setiap kali posisi lat/lng kurir berubah
    LaunchedEffect(courierLat, courierLng) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(LatLng(courierLat, courierLng), 16f),
            durationMs = 1000
        )
    }

    // Listener posisi kurir dari Firestore (untuk Nasabah / Super Admin)
    DisposableEffect(isAdminMode, courierIdToTrack) {
        val listener = if (courierIdToTrack == "all") {
            // Multi-Courier listener handled inside map
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
                            // Filter data lama Surabaya agar tidak muncul
                            !(Math.abs(lat - (-7.2575)) < 0.001 && Math.abs(lng - 112.7521) < 0.001)
                        }
                    }
                }
        } else null
        onDispose { listener?.remove() }
    }

    // Fused Location Client untuk Admin/Kurir/SuperAdmin yang memancarkan GPS
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Ambil GPS langsung saat screen dimuat (terlepas dari mode admin/nasabah)
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
                        if (isAdminMode || courierIdToTrack == "all") {
                            coroutineScope.launch {
                                val uid = auth.currentUser?.uid ?: "user_siram"
                                val name = auth.currentUser?.displayName?.ifBlank { "Super Admin (Anda)" } ?: "Super Admin (Anda)"
                                fleetRepo.updateLocation(uid, name, loc.latitude, loc.longitude)
                            }
                        }
                    }
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { loc ->
                    if (loc != null) {
                        if (isAdminMode || courierIdToTrack.isBlank() || courierIdToTrack == "all") {
                            courierLat = loc.latitude
                            courierLng = loc.longitude
                        }
                        if (isAdminMode || courierIdToTrack == "all") {
                            coroutineScope.launch {
                                val uid = auth.currentUser?.uid ?: "user_siram"
                                val name = auth.currentUser?.displayName?.ifBlank { "Super Admin (Anda)" } ?: "Super Admin (Anda)"
                                fleetRepo.updateLocation(uid, name, loc.latitude, loc.longitude)
                            }
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
                            Log.d("SIRAM_GPS", "Live GPS Broadcasted: ${location.latitude}, ${location.longitude}")
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

    // Animasi Pulse untuk Indikator Live Location ala WhatsApp
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
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
                title = { Text(if (isAdminMode) "Siar Live GPS Armada" else "Lacak Posisi Kurir", fontWeight = FontWeight.Bold) },
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
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // TITIK JEMPUT NASABAH (DESTINATION PIN) JIKA ADA
                if (userPickupLat != 0.0 && userPickupLng != 0.0) {
                    val pickupMarkerState = rememberMarkerState(key = "user_pickup_dest", position = LatLng(userPickupLat, userPickupLng))
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
                                snippet = "Klik untuk rincian kurir",
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
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1D4ED8))
                                        )
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
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1D4ED8))
                                )
                            }
                        }
                    }
                }
            }

            // WHATSAPP STYLE LIVE LOCATION PULSING INDICATOR BADGE
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(50.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = pulseAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1D4ED8))
                        )
                    }
                    Text(
                        text = if (isAdminMode) "🟢 Live GPS: Menyiarkan Lokasi Anda" else "🔵 Live Location: $courierName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // POPUP RINCIAN KURIR KETIKA LINGKARAN BIRU DIKLIK (KHUSUS SUPER ADMIN)
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
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0F2FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0284C7))
                                }
                                Column {
                                    Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Text(text = "🟢 Status: Aktif Siar Live GPS", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(onClick = { selectedCourierInfo = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = String.format("📍 Posisi GPS: %.5f, %.5f", lat, lng),
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            if (phone.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val formatted = NotificationHelper.formatPhoneNumberForWhatsApp(phone)
                                        val url = "https://api.whatsapp.com/send?phone=$formatted&text=${Uri.encode("Halo $name, saya Super Admin SIRAM...")}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
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
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri)
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        // ignore
                                    }
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

            // WHATSAPP STYLE LIVE LOCATION PULSING INDICATOR BADGE
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(50.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = pulseAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1D4ED8))
                        )
                    }
                    Text(
                        text = if (isAdminMode) "🟢 Live GPS: Menyiarkan Lokasi Anda" else "🔵 Live Location: $courierName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }
    }
}
