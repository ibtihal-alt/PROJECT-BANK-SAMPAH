package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import java.io.File

// PALET WARNA KOMERSIAL (LIGHT THEME)
private val PrimaryGreen = Color(0xFF006C4C)
private val PrimaryGradient = Color(0xFF00A86B)
private val BackgroundLight = Color(0xFFDCF0E0)
private val CardBackground = Color(0xFFFFFFFF)
private val ButterYellow = Color(0xFFF7CF68)
private val ButterYellowDark = Color(0xFF2E2405)

private val RedDanger = Color(0xFFDC2626)
private val RedDangerBg = Color(0xFFFEE2E2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    themePreferences: ThemePreferences,
    onMutasiSaldoClick: () -> Unit = {},
    onEcoEducationClick: () -> Unit = {},
    onHelpCenterClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }

    val auth = remember { FirebaseAuth.getInstance() }
    val user = auth.currentUser

    val namaUser = user?.displayName?.ifBlank { "Nasabah SIRAM" } ?: "Nasabah SIRAM"
    val emailUser = user?.email ?: "email@domain.com"

    var showEditHpDialog by remember { mutableStateOf(false) }
    var inputHp by remember { mutableStateOf(userPrefs.nomorHp) }

    var showEditAlamatDialog by remember { mutableStateOf(false) }
    var inputAlamat by remember { mutableStateOf(userPrefs.alamatDefault) }

    // State untuk Crop Foto Dialog
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    // Launcher untuk memilih foto dari galeri HP
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingImageUri = it
            showCropDialog = true
        }
    }

    val scrollState = rememberScrollState()

    // Dialog Crop Foto Manual
    if (showCropDialog && pendingImageUri != null) {
        ImageCropDialog(
            imageUri = pendingImageUri!!,
            onDismiss = {
                showCropDialog = false
                pendingImageUri = null
            },
            onCropConfirmed = { uriStr ->
                userPrefs.saveFotoProfilLokal(Uri.parse(uriStr))
                showCropDialog = false
                pendingImageUri = null
            }
        )
    }

    // Dialog Edit No HP
    if (showEditHpDialog) {
        AlertDialog(
            onDismissRequest = { showEditHpDialog = false },
            title = { Text("Atur Nomor WhatsApp / HP", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputHp,
                    onValueChange = { inputHp = it },
                    label = { Text("Nomor HP / WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        userPrefs.saveUserHp(inputHp.trim())
                        showEditHpDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("SIMPAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditHpDialog = false }) { Text("BATAL") }
            }
        )
    }

    // Dialog Edit Alamat Default
    if (showEditAlamatDialog) {
        AlertDialog(
            onDismissRequest = { showEditAlamatDialog = false },
            title = { Text("Atur Alamat Penjemputan Default", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputAlamat,
                    onValueChange = { inputAlamat = it },
                    label = { Text("Alamat Lengkap") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        userPrefs.saveAlamatDefault(inputAlamat.trim())
                        showEditAlamatDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("SIMPAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAlamatDialog = false }) { Text("BATAL") }
            }
        )
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Profil Pengguna", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A)) },
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable {
                                    galleryLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (userPrefs.fotoProfil.isNotBlank()) {
                                AsyncImage(
                                    model = if (userPrefs.fotoProfil.startsWith("/")) File(userPrefs.fotoProfil) else Uri.parse(userPrefs.fotoProfil),
                                    contentDescription = "Foto Profil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = namaUser,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "Nasabah Aktif Bank Sampah",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        IconButton(onClick = {
                            galleryLauncher.launch("image/*")
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Ganti Foto dari HP", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // KARTU INFORMASI AKUN
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rincian Kontak & Identitas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ketuk untuk ubah",
                                    fontSize = 11.sp,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            CommercialProfileInfoRow(
                                icon = Icons.Default.Email,
                                label = "Alamat Email",
                                value = emailUser
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            CommercialProfileInfoRow(
                                icon = Icons.Default.Phone,
                                label = "Nomor WhatsApp / HP",
                                value = userPrefs.nomorHp.ifBlank { "Belum diatur (Ketuk untuk atur)" },
                                onClick = {
                                    inputHp = userPrefs.nomorHp
                                    showEditHpDialog = true
                                }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            CommercialProfileInfoRow(
                                icon = Icons.Default.Home,
                                label = "Alamat Penjemputan Default",
                                value = userPrefs.alamatDefault.ifBlank { "Belum diatur (Ketuk untuk atur)" },
                                onClick = {
                                    inputAlamat = userPrefs.alamatDefault
                                    showEditAlamatDialog = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MENU INFORMASI & BANTUAN (MUTASI, EDUKASI, BANTUAN)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Informasi & Bantuan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            CommercialProfileInfoRow(
                                icon = Icons.Default.AccountBalanceWallet,
                                label = "Mutasi Saldo & Rekening",
                                value = "Lihat riwayat kredit & debit",
                                onClick = onMutasiSaldoClick
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            CommercialProfileInfoRow(
                                icon = Icons.Default.Eco,
                                label = "Edukasi & Tips Daur Ulang",
                                value = "Panduan ramah lingkungan",
                                onClick = onEcoEducationClick
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            CommercialProfileInfoRow(
                                icon = Icons.Default.HelpOutline,
                                label = "Pusat Bantuan & FAQ",
                                value = "Hubungi CS dan tanya jawab",
                                onClick = onHelpCenterClick
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // INFO KEAMANAN AKUN
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Keamanan Akun Terjamin",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Akun Anda dilindungi enkripsi Firebase Authentication.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // TOMBOL LOGOUT
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RedDangerBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLogoutClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(RedDanger.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Logout",
                                        tint = RedDanger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Keluar dari Akun",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = RedDanger
                                    )
                                    Text(
                                        text = "Akhiri sesi penggunaan aplikasi SIRAM",
                                        fontSize = 11.sp,
                                        color = RedDanger.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = RedDanger
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirmed: (String) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sesuaikan / Crop Foto Profil", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Seret untuk geser dan gunakan gestur cubit (pinch) untuk zoom agar foto pas di dalam bingkai.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Crop Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCropConfirmed(imageUri.toString())
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("GUNAKAN FOTO INI")
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
private fun CommercialProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = if (onClick != null) 4.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
