package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityCriteriaWeights
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityRankedItem
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PriorityTier
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val PrimaryGreen = Color(0xFF006C4C)
private val PrimaryGradient = Color(0xFF00A86B)
private val BackgroundLight = Color(0xFFF8FAFC)
private val CardBackground = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionSupportScreen(
    viewModel: SampahViewModel,
    onNavigateToMapWithPickup: (pickupId: Int, lat: Double, lng: Double, address: String) -> Unit = { _, _, _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }

    val daftarPickupAdmin by viewModel.semuaPenjemputanAdmin.collectAsState(initial = emptyList())
    val criteriaWeights by viewModel.criteriaWeights.collectAsState()

    var fleetLat by remember { mutableStateOf(-7.4029) } // Default Ngawi
    var fleetLng by remember { mutableStateOf(111.4452) }
    var fleetCapacityKg by remember { mutableStateOf(500.0) }
    var currentPayloadKg by remember { mutableStateOf(120.0) }
    var selectedSortMode by remember { mutableStateOf("SPK") } // SPK, DISTANCE, WAIT_TIME, FILL_LEVEL
    var showWeightsDialog by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<PriorityRankedItem?>(null) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedPickupForAssign by remember { mutableStateOf<PickupTransaction?>(null) }
    var staffList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Ambil daftar staf kurir
    LaunchedEffect(Unit) {
        try {
            val usersSnap = firestore.collection("users").get().await()
            val list = mutableListOf<Map<String, Any>>()
            for (doc in usersSnap.documents) {
                val role = doc.getString("role") ?: "user"
                if (role.equals("admin", ignoreCase = true) || role.equals("super_admin", ignoreCase = true)) {
                    val m = doc.data?.toMutableMap() ?: mutableMapOf()
                    m["uid"] = doc.id
                    list.add(m)
                }
            }
            staffList = list
        } catch (e: Exception) {
            // ignore
        }
    }

    // Ambil lokasi GPS real-time armada kurir / admin saat ini
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
                        fleetLat = loc.latitude
                        fleetLng = loc.longitude
                    }
                }
            } catch (e: SecurityException) {
                // ignore
            }
        }
    }

    // Ambil data penjemputan yang masih butuh tindakan (MENUNGGU atau DIPROSES)
    val activePickups = remember(daftarPickupAdmin) {
        daftarPickupAdmin.filter { !it.status.equals("SELESAI", ignoreCase = true) }
    }

    // Hitung peringkat SPK AWCP
    val rankedItems = remember(activePickups, fleetLat, fleetLng, fleetCapacityKg, currentPayloadKg, criteriaWeights, selectedSortMode) {
        val baseRanked = AdaptivePriorityEngine.rankPickups(
            pickups = activePickups,
            fleetLat = fleetLat,
            fleetLng = fleetLng,
            fleetCapacityKg = fleetCapacityKg,
            currentPayloadKg = currentPayloadKg,
            weights = criteriaWeights
        )

        when (selectedSortMode) {
            "DISTANCE" -> baseRanked.sortedBy { it.breakdown.distanceKm }
                .mapIndexed { idx, itm -> itm.copy(rank = idx + 1) }
            "WAIT_TIME" -> baseRanked.sortedByDescending { it.breakdown.waitingHours }
                .mapIndexed { idx, itm -> itm.copy(rank = idx + 1) }
            "FILL_LEVEL" -> baseRanked.sortedByDescending { it.pickup.tingkatKepenuhan }
                .mapIndexed { idx, itm -> itm.copy(rank = idx + 1) }
            else -> baseRanked // SPK AWCP Score default
        }
    }

    val criticalCount = remember(rankedItems) { rankedItems.count { it.breakdown.tier == PriorityTier.CRITICAL } }
    val highCount = remember(rankedItems) { rankedItems.count { it.breakdown.tier == PriorityTier.HIGH } }
    val avgScore = remember(rankedItems) {
        if (rankedItems.isNotEmpty()) rankedItems.map { it.breakdown.totalScore }.average() else 0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SPK Prioritas Penjemputan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Adaptive Context-Aware Priority Engine",
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
                    IconButton(onClick = { showWeightsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Sesuaikan Bobot SPK",
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
                .background(BackgroundLight)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // BANNER HERO EDUKATIF / SISTEM OVERVIEW
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF004D36), PrimaryGreen, Color(0xFF00A86B))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.2f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🎯", fontSize = 16.sp)
                                            }
                                        }
                                        Text(
                                            text = "Decision Support System",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Surface(
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Real-time Multi-Criteria",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Menentukan titik penjemputan prioritas berdasarkan kepenuhan tempat sampah, waktu menunggu, jenis sampah, kedekatan armada, dan urgensi warga. Kondisi darurat didahulukan meskipun tidak paling dekat.",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                // KPI METRIC CARDS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricSummaryCard(
                            title = "Total Titik",
                            value = "${rankedItems.size}",
                            subtitle = "Perlu dijemput",
                            badgeColor = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Darurat Kritis",
                            value = "$criticalCount",
                            subtitle = "Skor AWCP ≥ 80",
                            badgeColor = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Prioritas Tinggi",
                            value = "$highCount",
                            subtitle = "Skor 65 - 79",
                            badgeColor = Color(0xFFEA580C),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ARMADA STATE & SIMULATION CHIP
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                    Text("Status Armada Lapangan:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                }
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🟢 GPS Aktif",
                                        color = Color(0xFF15803D),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📍 Posisi: ${String.format("%.4f", fleetLat)}, ${String.format("%.4f", fleetLng)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "📦 Sisa Kapasitas: ${(fleetCapacityKg - currentPayloadKg).toInt()} / ${fleetCapacityKg.toInt()} Kg",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                // FILTER & SORT MODES
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Metode Pengurutan Rute Armada:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedSortMode == "SPK",
                                    onClick = { selectedSortMode = "SPK" },
                                    label = { Text("⚡ Rekomendasi SPK (AWCP Score)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedSortMode == "DISTANCE",
                                    onClick = { selectedSortMode = "DISTANCE" },
                                    label = { Text("📏 Jarak Terdekat Saja (Konvensional)", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedSortMode == "FILL_LEVEL",
                                    onClick = { selectedSortMode = "FILL_LEVEL" },
                                    label = { Text("🗑️ Kepenuhan Tertinggi", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedSortMode == "WAIT_TIME",
                                    onClick = { selectedSortMode = "WAIT_TIME" },
                                    label = { Text("⏳ Menunggu Terlama", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // NOTICE TRADE-OFF JIKA SPK DIPILIH
                if (selectedSortMode == "SPK" && rankedItems.size >= 2) {
                    item {
                        val first = rankedItems[0]
                        val second = rankedItems[1]
                        val explanation = AdaptivePriorityEngine.explainTradeoff(first, second)
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("💡", fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = "Penalaran Cerdas Sistem Pendukung Keputusan:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = explanation,
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E3A8A),
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // DAFTAR TITIK PENJEMPUTAN TERURUT
                if (rankedItems.isEmpty()) {
                    item {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎉", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tidak ada antrean penjemputan aktif", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Semua permintaan warga telah selesai diproses oleh armada.", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                } else {
                    items(rankedItems, key = { it.pickup.id }) { rankedItem ->
                        PriorityRankedItemCard(
                            item = rankedItem,
                            onDetailClick = { selectedItemForDetail = rankedItem },
                            onAssignClick = {
                                selectedPickupForAssign = rankedItem.pickup
                                showAssignDialog = true
                            },
                            onNavigateClick = {
                                onNavigateToMapWithPickup(
                                    rankedItem.pickup.id,
                                    rankedItem.pickup.latitude,
                                    rankedItem.pickup.longitude,
                                    rankedItem.pickup.alamatLengkap
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // MODAL DIALOG PENYESUAIAN BOBOT KRITERIA (SUPER ADMIN / DISPATCHER)
    if (showWeightsDialog) {
        WeightsConfigDialog(
            currentWeights = criteriaWeights,
            onSave = { newWeights ->
                viewModel.updateCriteriaWeights(newWeights)
                showWeightsDialog = false
            },
            onReset = {
                viewModel.resetCriteriaWeights()
                showWeightsDialog = false
            },
            onDismiss = { showWeightsDialog = false }
        )
    }

    // MODAL DETAIL BREAKDOWN AWCP SCORE
    selectedItemForDetail?.let { item ->
        AwcpScoreDetailDialog(
            item = item,
            onDismiss = { selectedItemForDetail = null },
            onNavigateToMap = {
                onNavigateToMapWithPickup(
                    item.pickup.id,
                    item.pickup.latitude,
                    item.pickup.longitude,
                    item.pickup.alamatLengkap
                )
                selectedItemForDetail = null
            }
        )
    }

    // MODAL ASSIGN KURIR
    if (showAssignDialog && selectedPickupForAssign != null) {
        val pickup = selectedPickupForAssign!!
        Dialog(onDismissRequest = { showAssignDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Tugaskan Armada Lapangan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Titik: ${pickup.alamatLengkap} (${pickup.jenisSampah})",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(staffList) { staff ->
                            val uid = staff["uid"] as? String ?: ""
                            val nama = staff["nama"] as? String ?: "Kurir Lapangan"
                            val role = staff["role"] as? String ?: "admin"

                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.assignKurirPenjemputan(pickup, uid, nama)
                                        showAssignDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = PrimaryGreen.copy(alpha = 0.15f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text(nama, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(role.uppercase(), fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showAssignDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
            Text(text = subtitle, fontSize = 9.sp, color = Color(0xFF64748B), maxLines = 1)
        }
    }
}

@Composable
private fun PriorityRankedItemCard(
    item: PriorityRankedItem,
    onDetailClick: () -> Unit,
    onAssignClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    val b = item.breakdown
    val p = item.pickup

    val tierBadgeColor = Color(b.tier.badgeColorHex)
    val tierContainerColor = Color(b.tier.containerColorHex)

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // HEADER BARIS ATAS: RANK, TIER, SCORE BADGE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (item.rank == 1) Color(0xFFDC2626) else Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${item.rank}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Surface(
                        color = tierContainerColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = b.tier.label,
                            color = tierBadgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // SCORE GAUGE PILL
                Surface(
                    color = tierBadgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tierBadgeColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("AWCP:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        Text(
                            text = String.format("%.1f", b.totalScore),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierBadgeColor
                        )
                        Text("/100", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ALAMAT DAN KATEGORI SAMPAH
            Text(
                text = p.alamatLengkap,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // TAGS CHIPS KRITERIA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallTag(text = "♻️ ${p.jenisSampah}", bgColor = Color(0xFFF1F5F9), textColor = Color(0xFF334155))
                SmallTag(
                    text = "🗑️ Kepenuhan ${p.tingkatKepenuhan}%",
                    bgColor = if (p.tingkatKepenuhan >= 85) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                    textColor = if (p.tingkatKepenuhan >= 85) Color(0xFFDC2626) else Color(0xFFD97706)
                )
                SmallTag(
                    text = "⏳ ${String.format("%.1f", b.waitingHours)} Jam",
                    bgColor = Color(0xFFF1F5F9),
                    textColor = Color(0xFF334155)
                )
                SmallTag(
                    text = "📏 ${String.format("%.1f", b.distanceKm)} km",
                    bgColor = Color(0xFFE0F2FE),
                    textColor = Color(0xFF0369A1)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RINGKASAN PENALARAN SPK (REASONING AI)
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("🤖", fontSize = 14.sp)
                    Text(
                        text = b.reasoningSummary,
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TOMBOL AKSI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDetailClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Detail Skor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Peta Rute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onAssignClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun SmallTag(text: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AwcpScoreDetailDialog(
    item: PriorityRankedItem,
    onDismiss: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    val b = item.breakdown
    val p = item.pickup

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rincian Skor AWCP (#${item.rank})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = p.alamatLengkap,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // PROGRESS BAR CRITERIA BREAKDOWN
                CriteriaScoreRow(label = "Kepenuhan (${p.tingkatKepenuhan}%)", score = b.fillScore, weight = 0.25)
                CriteriaScoreRow(label = "Waktu Tunggu (${String.format("%.1f", b.waitingHours)} jam)", score = b.waitScore, weight = 0.20)
                CriteriaScoreRow(label = "Karakteristik Sampah (${p.jenisSampah})", score = b.wasteScore, weight = 0.15)
                CriteriaScoreRow(label = "Urgensi Warga (${p.urgensiWarga})", score = b.urgencyScore, weight = 0.20)
                CriteriaScoreRow(label = "Kedekatan Armada (${String.format("%.1f", b.distanceKm)} km)", score = b.proximityScore, weight = 0.10)
                CriteriaScoreRow(label = "Kecocokan Muatan Armada", score = b.capacityScore, weight = 0.10)

                if (b.contextBoostApplied > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚡ Context Boost: +${String.format("%.1f", b.contextBoostApplied)} poin (Kondisi Kritis Lingkungan)",
                            color = Color(0xFFB45309),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // FAKTOR PENENTU
                if (b.explanationDetails.isNotEmpty()) {
                    Text("Faktor Penentu Algoritma:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(4.dp))
                    b.explanationDetails.forEach { exp ->
                        Row(modifier = Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            Text(exp, fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToMap,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Navigasi ke Titik Ini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CriteriaScoreRow(label: String, score: Double, weight: Double) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = Color(0xFF334155))
            Text(
                text = "${score.toInt()} pts (${(weight * 100).toInt()}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (score / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (score >= 80) Color(0xFFDC2626) else if (score >= 60) Color(0xFFEA580C) else PrimaryGreen,
            trackColor = Color(0xFFE2E8F0)
        )
    }
}

@Composable
private fun WeightsConfigDialog(
    currentWeights: PriorityCriteriaWeights,
    onSave: (PriorityCriteriaWeights) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var wFill by remember { mutableStateOf(currentWeights.weightFill.toFloat()) }
    var wWait by remember { mutableStateOf(currentWeights.weightWait.toFloat()) }
    var wWaste by remember { mutableStateOf(currentWeights.weightWaste.toFloat()) }
    var wUrgency by remember { mutableStateOf(currentWeights.weightUrgency.toFloat()) }
    var wProx by remember { mutableStateOf(currentWeights.weightProximity.toFloat()) }
    var wCap by remember { mutableStateOf(currentWeights.weightCapacity.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Pengaturan Bobot SPK (MCDM)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Sesuaikan sensitivitas kriteria algoritma pengambilan keputusan.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                WeightSliderItem(label = "Kepenuhan Tempat Sampah", value = wFill, onValueChange = { wFill = it })
                WeightSliderItem(label = "Lama Waktu Menunggu", value = wWait, onValueChange = { wWait = it })
                WeightSliderItem(label = "Bahaya Jenis Sampah (Organik/B3)", value = wWaste, onValueChange = { wWaste = it })
                WeightSliderItem(label = "Urgensi Laporan Warga", value = wUrgency, onValueChange = { wUrgency = it })
                WeightSliderItem(label = "Kedekatan Jarak Armada", value = wProx, onValueChange = { wProx = it })
                WeightSliderItem(label = "Kapasitas Muatan Kendaraan", value = wCap, onValueChange = { wCap = it })

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset Default", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val total = (wFill + wWait + wWaste + wUrgency + wProx + wCap).coerceAtLeast(0.01f)
                            // Normalisasi total = 1.0
                            val normalized = PriorityCriteriaWeights(
                                weightFill = (wFill / total).toDouble(),
                                weightWait = (wWait / total).toDouble(),
                                weightWaste = (wWaste / total).toDouble(),
                                weightUrgency = (wUrgency / total).toDouble(),
                                weightProximity = (wProx / total).toDouble(),
                                weightCapacity = (wCap / total).toDouble()
                            )
                            onSave(normalized)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Terapkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightSliderItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = Color(0xFF334155))
            Text("${(value * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.05f..0.50f,
            colors = SliderDefaults.colors(
                thumbColor = PrimaryGreen,
                activeTrackColor = PrimaryGreen
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
