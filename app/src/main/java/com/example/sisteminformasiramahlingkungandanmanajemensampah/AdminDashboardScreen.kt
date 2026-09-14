package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PenarikanSaldo
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah
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
    userRole: String = "admin",
    onPriceManagerClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var selectedBottomTab by remember { mutableStateOf(0) }

    val daftarPickupAdmin by viewModel.semuaPenjemputanAdmin.collectAsState(initial = emptyList())
    val daftarSampahAdmin by viewModel.semuaSampahAdmin.collectAsState(initial = emptyList())
    val daftarPenarikanAdmin by viewModel.semuaPenarikanAdmin.collectAsState(initial = emptyList())

    var selectedFilterPickup by remember { mutableStateOf("MENUNGGU") }
    var selectedFilterSampah by remember { mutableStateOf("MENUNGGU") }
    var selectedFilterPenarikan by remember { mutableStateOf("MENUNGGU") }
    var searchQuery by remember { mutableStateOf("") }
    val isSuperAdmin = userRole.equals("super_admin", ignoreCase = true)

    var showSelesaiDialog by remember { mutableStateOf(false) }
    var selectedPickupForSelesai by remember { mutableStateOf<PickupTransaction?>(null) }

    val listStatusPickup = listOf("MENUNGGU", "DIPROSES", "SELESAI", "DITOLAK", "SEMUA")
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

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = CardBackground, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                    label = { Text("Jemput", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 1,
                    onClick = { selectedBottomTab = 1 },
                    icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                    label = { Text("Catat", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 2,
                    onClick = { selectedBottomTab = 2 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Pencairan", fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isSuperAdmin) "Super Admin Panel" else "Admin Dashboard",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = when (selectedBottomTab) {
                                        0 -> "Validasi Penjemputan Sampah Nasabah"
                                        1 -> "Validasi Input Sampah Mandiri"
                                        else -> "Validasi Transfer Pencairan Saldo"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onPriceManagerClick,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = "Kelola Harga",
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = onLogoutClick,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Logout",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        if (isSuperAdmin) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "Otoritas Sistem: Super Admin (Akses Penuh)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

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
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Terkumpul", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = String.format("%.1f Kg", totalTonaseSelesai),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(36.dp), color = Color(0xFFE2E8F0))
                        Column {
                            Text("Total Saldo Tervalidasi", fontSize = 11.sp, color = Color(0xFF64748B))
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
                    placeholder = { Text("Cari jenis sampah, alamat, no hp...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    },
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
                        .padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                when (selectedBottomTab) {
                    0 -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProfessionalMetricCard(
                                title = "Menunggu",
                                count = daftarPickupAdmin.count { it.status.equals("MENUNGGU", ignoreCase = true) },
                                accentColor = OrangePending,
                                bgColor = OrangePendingBg,
                                icon = Icons.Default.PendingActions,
                                modifier = Modifier.weight(1f)
                            )
                            ProfessionalMetricCard(
                                title = "Diproses",
                                count = daftarPickupAdmin.count { it.status.equals("DIPROSES", ignoreCase = true) },
                                accentColor = BlueProcess,
                                bgColor = BlueProcessBg,
                                icon = Icons.Default.LocalShipping,
                                modifier = Modifier.weight(1f)
                            )
                            ProfessionalMetricCard(
                                title = "Selesai",
                                count = daftarPickupAdmin.count { it.status.equals("SELESAI", ignoreCase = true) },
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
                                    text = {
                                        Text(
                                            text = filter,
                                            fontWeight = if (selectedFilterPickup == filter) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (selectedFilterPickup == filter) PrimaryGreen else Color(0xFF64748B)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                            if (filteredPickupList.isEmpty()) {
                                EmptyStateView(message = if (searchQuery.isNotEmpty()) "Tidak ditemukan hasil pencarian." else "Tidak ada transaksi di tab '$selectedFilterPickup'")
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
                                    text = {
                                        Text(
                                            text = filter,
                                            fontWeight = if (selectedFilterSampah == filter) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (selectedFilterSampah == filter) PrimaryGreen else Color(0xFF64748B)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                            if (filteredSampahList.isEmpty()) {
                                EmptyStateView(message = if (searchQuery.isNotEmpty()) "Tidak ditemukan hasil pencarian." else "Tidak ada catat sampah di tab '$selectedFilterSampah'")
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
                                                viewModel.updateStatusSampahMandiriByAdmin(item, statusBaru)
                                                selectedFilterSampah = statusBaru
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
                                    text = {
                                        Text(
                                            text = filter,
                                            fontWeight = if (selectedFilterPenarikan == filter) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (selectedFilterPenarikan == filter) PrimaryGreen else Color(0xFF64748B)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                            if (filteredPenarikanList.isEmpty()) {
                                EmptyStateView(message = if (searchQuery.isNotEmpty()) "Tidak ditemukan hasil pencarian." else "Tidak ada pengajuan penarikan di tab '$selectedFilterPenarikan'")
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
                                                viewModel.updateStatusPenarikanByAdmin(item, statusBaru)
                                                selectedFilterPenarikan = statusBaru
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

    if (showSelesaiDialog && selectedPickupForSelesai != null) {
        KalkulatorLapanganDialog(
            pickup = selectedPickupForSelesai!!,
            onDismiss = {
                showSelesaiDialog = false
                selectedPickupForSelesai = null
            },
            onConfirmSelesai = { beratAktual, hargaPerKg, totalSaldo ->
                val pickupUpdated = selectedPickupForSelesai!!.copy(
                    beratAktualKg = beratAktual,
                    hargaPerKgAktual = hargaPerKg,
                    totalNominalRp = totalSaldo,
                    status = "SELESAI"
                )

                viewModel.updateStatusPenjemputanByAdmin(pickupUpdated, "SELESAI")
                viewModel.simpanSampah(
                    jenisSampah = pickupUpdated.jenisSampah,
                    berat = beratAktual,
                    catatan = "Hasil Penjemputan Admin (Alamat: ${pickupUpdated.alamatLengkap})",
                    hargaPerKgCustom = hargaPerKg
                )

                showSelesaiDialog = false
                selectedPickupForSelesai = null
                selectedFilterPickup = "SELESAI"
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
    var beratAktualText by remember { mutableStateOf(pickup.estimasiBeratKg.toString()) }
    var hargaPerKgText by remember { mutableStateOf("5000") }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val beratDouble = beratAktualText.toDoubleOrNull() ?: 0.0
    val hargaDouble = hargaPerKgText.toDoubleOrNull() ?: 0.0
    val totalEstimasi = beratDouble * hargaDouble

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚖️ Kalkulator Timbangan Lapangan",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Verifikasi berat dan harga riil untuk jenis sampah: ${pickup.jenisSampah}",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

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
                onClick = {
                    if (beratDouble > 0 && hargaDouble > 0) {
                        onConfirmSelesai(beratDouble, hargaDouble, totalEstimasi)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
            ) {
                Text("VERIFIKASI & SELESAI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("BATAL")
            }
        }
    )
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF64748B)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = "$count",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = pickup.jenisSampah,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (pickup.beratAktualKg > 0) "Aktual: ${pickup.beratAktualKg} Kg" else "Estimasi: ${pickup.estimasiBeratKg} Kg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )
                    }
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = pickup.status.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = pickup.alamatLengkap,
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                }

                IconButton(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(pickup.alamatLengkap)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Maps Navigation",
                        tint = BlueProcess,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = pickup.nomorHp,
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                }

                IconButton(
                    onClick = {
                        val formattedHp = if (pickup.nomorHp.startsWith("0")) "62" + pickup.nomorHp.substring(1) else pickup.nomorHp
                        val url = "https://api.whatsapp.com/send?phone=$formattedHp&text=${Uri.encode("Halo Kak, saya Admin SIRAM mau konfirmasi terkait penjemputan sampah ${pickup.jenisSampah}.")}"
                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(waIntent)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp Direct Contact",
                        tint = GreenSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (pickup.catatan.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Catatan: ${pickup.catatan}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onUpdateStatus(pickup, "DIPROSES") },
                    enabled = !pickup.status.equals("DIPROSES", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueProcess,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Text("PROSES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onUpdateStatus(pickup, "SELESAI") },
                    enabled = !pickup.status.equals("SELESAI", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenSuccess,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Text("SELESAI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onUpdateStatus(pickup, "DITOLAK") },
                    enabled = !pickup.status.equals("DITOLAK", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedDanger,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Text("TOLAK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Recycling,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = sampah.jenisSampah,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Berat: ${sampah.berat} Kg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )
                    }
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = sampah.status.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Estimasi Saldo Tambahan: ${currencyFormat.format(sampah.totalNilai)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            if (sampah.catatan.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Catatan: ${sampah.catatan}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onValidasi(sampah, "DISETUJUI") },
                    enabled = !sampah.status.equals("DISETUJUI", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenSuccess,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SETUJUI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onValidasi(sampah, "DITOLAK") },
                    enabled = !sampah.status.equals("DITOLAK", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedDanger,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.HighlightOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = penarikan.metodePenarikan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "No: ${penarikan.nomorTujuan}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = penarikan.status.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Atas Nama: ${penarikan.namaPemilik}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )

            Text(
                text = "Nominal Transfer: ${currencyFormat.format(penarikan.nominalRp)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onValidasi(penarikan, "SUKSES") },
                    enabled = !penarikan.status.equals("SUKSES", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenSuccess,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Text("TRANSFER SUKSES", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onValidasi(penarikan, "DITOLAK") },
                    enabled = !penarikan.status.equals("DITOLAK", ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedDanger,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Text("TOLAK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}