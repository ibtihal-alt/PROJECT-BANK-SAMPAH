package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sisteminformasiramahlingkungandanmanajemensampah.ui.theme.SistemInformasiRamahLingkunganDanManajemenSampahTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val themePreferences = remember { ThemePreferences(context) }

            SistemInformasiRamahLingkunganDanManajemenSampahTheme(
                darkTheme = themePreferences.isDarkMode
            ) {

                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val sampahViewModel: SampahViewModel = viewModel(
                    factory = SampahViewModelFactory(context)
                )

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {

                    // =========================
                    // HOME
                    // =========================
                    composable("home") {
                        HomeScreen(
                            modifier = Modifier.fillMaxSize(),
                            onStartClick = {
                                navController.navigate("login")
                            }
                        )
                    }

                    // =========================
                    // LOGIN (MULTI-LEVEL ADMIN)
                    // =========================
                    composable("login") {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onRegisterClick = {
                                authViewModel.resetState()
                                navController.navigate("register")
                            },
                            onLoginSuccess = {
                                authViewModel.fetchUserRole { role ->
                                    val r = role.lowercase().ifBlank { "user" }
                                    navController.navigate("welcome_animation/$r") {
                                        popUpTo("login") {
                                            inclusive = true
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // =========================
                    // REGISTER
                    // =========================
                    composable("register") {
                        RegisterScreen(
                            authViewModel = authViewModel,
                            onRegisterSuccess = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // =========================
                    // TARIK SALDO NASABAH
                    // =========================
                    composable("tarik_saldo") {
                        val totalNilai by sampahViewModel.totalNilai.collectAsState(initial = 0.0)

                        TarikSaldoScreen(
                            saldoTersedia = totalNilai,
                            onAjukanTarikClick = { metode, nomor, nama, nominal ->
                                sampahViewModel.ajukanPenarikanSaldo(
                                    metode = metode,
                                    nomorTujuan = nomor,
                                    namaPemilik = nama,
                                    nominal = nominal
                                )
                                navController.popBackStack()
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // =========================
                    // DASHBOARD NASABAH
                    // =========================
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = sampahViewModel,
                            onCatatSampahClick = {
                                navController.navigate("catat_sampah")
                            },
                            onAiChatbotClick = {
                                navController.navigate("ai_chatbot")
                            },
                            onJemputSampahClick = {
                                navController.navigate("jemput_sampah")
                            },
                            onStatistikClick = {
                                navController.navigate("statistik")
                            },
                            onRiwayatClick = {
                                navController.navigate("riwayat_sampah")
                            },
                            onTarikSaldoClick = {
                                navController.navigate("tarik_saldo")
                            },
                            onProfileClick = {
                                navController.navigate("profile")
                            },
                            onRiwayatJemputClick = {
                                navController.navigate("riwayat_jemput")
                            },
                            onTrackClick = {
                                navController.navigate("tracking_map/false")
                            }
                        )
                    }

                    // =========================
                    // DASHBOARD ADMIN (KURIR)
                    // =========================
                    composable("admin_dashboard") {
                        AdminDashboardScreen(
                            viewModel = sampahViewModel,
                            onLogoutClick = {
                                authViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo("admin_dashboard") {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // =========================
                    // SUPER ADMIN DASHBOARD
                    // =========================
                    composable("super_admin_dashboard") {
                        SuperAdminDashboardScreen(
                            viewModel = sampahViewModel,
                            onPriceManagerClick = {
                                navController.navigate("admin_price_manager")
                            },
                            onTrackingClick = {
                                navController.navigate("tracking_map/all")
                            },
                            onBroadcastClick = {
                                navController.navigate("broadcast")
                            },
                            onLogoutClick = {
                                authViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo("super_admin_dashboard") {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // =========================
                    // BROADCAST SCREEN
                    // =========================
                    composable("broadcast") {
                        BroadcastScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    // =========================
                    // ADMIN PRICE MANAGER
                    // =========================
                    composable("admin_price_manager") {
                        AdminPriceScreen(
                            viewModel = sampahViewModel,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // =========================
                    // WELCOME ANIMATION SCREEN
                    // =========================
                    composable(
                        route = "welcome_animation/{role}",
                        arguments = listOf(navArgument("role") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: "user"
                        WelcomeAnimationScreen {
                            val destination = when (role) {
                                "super_admin" -> "super_admin_dashboard"
                                "admin" -> "admin_dashboard"
                                else -> "dashboard"
                            }
                            navController.navigate(destination) {
                                popUpTo("welcome_animation/{role}") {
                                    inclusive = true
                                }
                            }
                        }
                    }

                    // =========================
                    // FITUR PENJEMPUTAN SAMPAH
                    // =========================
                    composable("jemput_sampah") {
                        JemputSampahScreen(
                            viewModel = sampahViewModel,
                            onAjukanClick = { jenis, berat, alamat, hp, catatan, lat, lng ->
                                sampahViewModel.ajukanPenjemputan(
                                    jenisSampah = jenis,
                                    estimasiBeratKg = berat,
                                    alamatLengkap = alamat,
                                    nomorHp = hp,
                                    catatan = catatan,
                                    latitude = lat,
                                    longitude = lng
                                )
                                navController.navigate("riwayat_jemput") {
                                    popUpTo("jemput_sampah") { inclusive = true }
                                }
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("riwayat_jemput") {
                        RiwayatJemputScreen(
                            viewModel = sampahViewModel,
                            onTambahJemputClick = {
                                navController.navigate("jemput_sampah")
                            },
                            onTrackClick = { pickup ->
                                val courierId = pickup.assignedKurirId
                                val lat = pickup.latitude
                                val lng = pickup.longitude
                                val addr = Uri.encode(pickup.alamatLengkap)
                                navController.navigate("tracking_map/false?courierId=$courierId&lat=$lat&lng=$lng&addr=$addr")
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // =========================
                    // LACAK ARMADA GPS MAPS
                    // =========================
                    composable(
                        route = "tracking_map/{mode}?courierId={courierId}&lat={lat}&lng={lng}&addr={addr}",
                        arguments = listOf(
                            navArgument("mode") { type = NavType.StringType; defaultValue = "false" },
                            navArgument("courierId") { type = NavType.StringType; defaultValue = ""; nullable = true },
                            navArgument("lat") { type = NavType.FloatType; defaultValue = 0f },
                            navArgument("lng") { type = NavType.FloatType; defaultValue = 0f },
                            navArgument("addr") { type = NavType.StringType; defaultValue = ""; nullable = true }
                        )
                    ) { backStackEntry ->
                        val mode = backStackEntry.arguments?.getString("mode") ?: "false"
                        val courierId = backStackEntry.arguments?.getString("courierId") ?: ""
                        val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
                        val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 0.0
                        val addr = backStackEntry.arguments?.getString("addr") ?: ""
                        val isAdmin = mode == "true"
                        val isMulti = mode == "all"
                        TrackingMapScreen(
                            courierIdToTrack = if (isMulti) "all" else courierId,
                            isAdminMode = isAdmin,
                            userPickupLat = lat,
                            userPickupLng = lng,
                            userPickupAddress = addr,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // =========================
                    // AI CHATBOT ASSISTANT
                    // =========================
                    composable("ai_chatbot") {
                        AiChatbotScreen(
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // =========================
                    // STATISTIK
                    // =========================
                    composable("statistik") {
                        StatistikScreen(
                            viewModel = sampahViewModel
                        )
                    }

                    // =========================
                    // CATAT SAMPAH BIASA
                    // =========================
                    composable("catat_sampah") {
                        CatatSampahScreen(
                            viewModel = sampahViewModel,
                            onSimpanClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // =========================
                    // CATAT SAMPAH DENGAN PARAMETER
                    // =========================
                    composable(
                        route = "catat_sampah/{jenisSampah}",
                        arguments = listOf(
                            navArgument("jenisSampah") {
                                type = NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val jenisSampah = backStackEntry.arguments?.getString("jenisSampah") ?: ""

                        CatatSampahScreen(
                            viewModel = sampahViewModel,
                            jenisAwal = jenisSampah,
                            onSimpanClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // =========================
                    // RIWAYAT SAMPAH
                    // =========================
                    composable("riwayat_sampah") {
                        RiwayatSampahScreen(
                            viewModel = sampahViewModel,
                            onEditClick = { sampah ->
                                navController.navigate("edit_sampah/${sampah.id}")
                            }
                        )
                    }

                    // =========================
                    // EDIT SAMPAH
                    // =========================
                    composable(
                        route = "edit_sampah/{sampahId}",
                        arguments = listOf(
                            navArgument("sampahId") {
                                type = NavType.IntType
                            }
                        )
                    ) { backStackEntry ->
                        val sampahId = backStackEntry.arguments?.getInt("sampahId") ?: return@composable

                        val semuaSampah by sampahViewModel.semuaSampah.collectAsState(initial = emptyList())
                        val sampah = semuaSampah.find { it.id == sampahId } ?: return@composable

                        EditSampahScreen(
                            sampah = sampah,
                            onSimpanClick = { sampahBaru ->
                                sampahViewModel.updateSampah(sampahBaru)
                                navController.popBackStack()
                            }
                        )
                    }

                    // =========================
                    // PROFIL & PENGATURAN TEMA
                    // =========================
                    composable("profile") {
                        ProfileScreen(
                            themePreferences = themePreferences,
                            onMutasiSaldoClick = { navController.navigate("mutasi_saldo") },
                            onEcoEducationClick = { navController.navigate("eco_education") },
                            onHelpCenterClick = { navController.navigate("help_center") },
                            onLogoutClick = {
                                authViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo("home") {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    // =========================
                    // MUTASI SALDO, EDUKASI, BANTUAN
                    // =========================
                    composable("mutasi_saldo") {
                        MutasiSaldoScreen(
                            viewModel = sampahViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable("eco_education") {
                        EcoEducationScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable("help_center") {
                        HelpCenterScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}