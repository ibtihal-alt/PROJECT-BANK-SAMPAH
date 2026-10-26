package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

// PALET WARNA CLAYMORPHIC PASTEL MINT & BUTTER YELLOW (SESUAI GAMBAR REFERENSI)
private val PrimaryGreen = Color(0xFF2E7D32)
private val PrimaryGradient = Color(0xFF5AB678)
private val BackgroundLight = Color(0xFFDCF0E0)
private val CardBackground = Color(0xFFFFFFFF)
private val ButterYellow = Color(0xFFF7CF68)
private val ButterYellowDark = Color(0xFF2E2405)

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onRegisterClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var googleErrorMessage by remember { mutableStateOf<String?>(null) }
    val authState by authViewModel.authState.collectAsState()

    val scrollState = rememberScrollState()

    // Helper Web Client ID untuk Firebase Google Sign-In
    val webClientId = remember(context) {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else "22813556115-siram.apps.googleusercontent.com"
    }

    val googleSignInClient = remember(context, webClientId) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleErrorMessage = null
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                authViewModel.loginWithGoogle(idToken)
            } else {
                googleErrorMessage = "Akun Google (${account?.email ?: "Terpilih"}) berhasil dipilih, namun ID Token belum aktif. Pastikan SHA-1 fingerprint terdaftar di Firebase Console."
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) { // 12501 = user canceled dialog
                googleErrorMessage = "Login Google belum berhasil (Kode: ${e.statusCode}). Silakan periksa koneksi internet atau konfigurasi Firebase."
            }
        } catch (e: Exception) {
            googleErrorMessage = "Gagal memproses akun Google: ${e.localizedMessage ?: "Terjadi kendala"}"
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.LoginSuccess) {
            authViewModel.resetState()
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // =========================
            // HERO BRANDING LOGO
            // =========================
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryGradient.copy(alpha = 0.2f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryGreen, PrimaryGradient)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Recycling,
                        contentDescription = "Logo SIRAM",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SIRAM",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryGreen,
                letterSpacing = 2.sp
            )

            Text(
                text = "Masuk ke akun Anda untuk mengelola sampah",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // =========================
            // FORM LOGIN CONTAINER
            // =========================
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Form Autentikasi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )

                    // INPUT EMAIL
                    SiramTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        placeholder = "Masukkan alamat email Anda",
                        keyboardType = KeyboardType.Email
                    )

                    // INPUT PASSWORD
                    SiramTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Masukkan password Anda",
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )

                    // PESAN ERROR JIKA GAGAL LOGIN
                    AnimatedVisibility(
                        visible = authState is AuthState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (authState is AuthState.Error) {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = (authState as AuthState.Error).message,
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // TOMBOL LOGIN (WARM BUTTER YELLOW PILL)
                    Button(
                        onClick = {
                            authViewModel.login(
                                email = email.trim(),
                                password = password.trim()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = authState !is AuthState.Loading,
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButterYellow,
                            contentColor = ButterYellowDark,
                            disabledContainerColor = ButterYellow.copy(alpha = 0.45f),
                            disabledContentColor = ButterYellowDark.copy(alpha = 0.5f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ButterYellowDark,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "MEMPROSES...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "MASUK KE AKUN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // =========================
                    // PEMISAH ATAU
                    // =========================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE2E8F0),
                            thickness = 1.dp
                        )
                        Text(
                            text = "ATAU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE2E8F0),
                            thickness = 1.dp
                        )
                    }

                    // =========================
                    // TOMBOL LOGIN LANGSUNG DENGAN GOOGLE
                    // =========================
                    OutlinedButton(
                        onClick = {
                            googleErrorMessage = null
                            try {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    googleAuthLauncher.launch(googleSignInClient.signInIntent)
                                }
                            } catch (e: Exception) {
                                googleErrorMessage = "Gagal membuka akun Google: ${e.localizedMessage}"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = authState !is AuthState.Loading,
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E293B)
                        ),
                        border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GoogleLogoIcon(modifier = Modifier.size(20.dp))
                            Text(
                                text = "Masuk Langsung dengan Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    // PESAN PERINGATAN / ERROR GOOGLE SIGN-IN
                    AnimatedVisibility(
                        visible = !googleErrorMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        googleErrorMessage?.let { msg ->
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "⚠️", fontSize = 14.sp)
                                    Text(
                                        text = msg,
                                        color = Color(0xFF92400E),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =========================
            // FOOTER REGISTER LINK
            // =========================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Belum memiliki akun?",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
                TextButton(
                    onClick = {
                        authViewModel.resetState()
                        onRegisterClick()
                    }
                ) {
                    Text(
                        text = "Daftar Sekarang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MULTI-ROLE BADGE INFO
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(50.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Mendukung Akses Nasabah, Admin, & Super Admin",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// =========================
// GOOGLE MULTI-COLOR LOGO ICON
// =========================
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val size = this.size.minDimension
        val strokeWidth = size * 0.22f
        val center = Offset(size / 2f, size / 2f)
        val radius = (size - strokeWidth) / 2f
        val rectSize = Size(radius * 2, radius * 2)
        val rectTopLeft = Offset(center.x - radius, center.y - radius)

        // Blue horizontal bar
        val barHeight = strokeWidth * 0.95f
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(center.x - strokeWidth * 0.2f, center.y - barHeight / 2f),
            size = Size(radius + strokeWidth * 0.2f, barHeight)
        )

        // Blue arc: from -45 to 45 deg
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 0f,
            sweepAngle = 45f,
            useCenter = false,
            topLeft = rectTopLeft,
            size = rectSize,
            style = Stroke(width = strokeWidth)
        )
        // Green arc: from 45 to 135 deg
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = rectTopLeft,
            size = rectSize,
            style = Stroke(width = strokeWidth)
        )
        // Yellow arc: from 135 to 225 deg
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = rectTopLeft,
            size = rectSize,
            style = Stroke(width = strokeWidth)
        )
        // Red arc: from 225 to 315 deg
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = rectTopLeft,
            size = rectSize,
            style = Stroke(width = strokeWidth)
        )
    }
}