package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

            SistemInformasiRamahLingkunganDanManajemenSampahTheme {

                val navController = rememberNavController()

                val authViewModel: AuthViewModel = viewModel()

                val context = LocalContext.current

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
                    // LOGIN
                    // =========================
                    composable("login") {

                        LoginScreen(
                            authViewModel = authViewModel,

                            onRegisterClick = {
                                authViewModel.resetState()
                                navController.navigate("register")
                            },

                            onLoginSuccess = {

                                navController.navigate("dashboard") {
                                    popUpTo("login") {
                                        inclusive = true
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
                    // DASHBOARD
                    // =========================
                    composable("dashboard") {

                        DashboardScreen(
                            viewModel = sampahViewModel,

                            onCatatSampahClick = {
                                navController.navigate("catat_sampah")
                            },

                            onAiScannerClick = {
                                navController.navigate("ai_scanner")
                            },

                            onStatistikClick = {
                                navController.navigate("statistik")
                            },

                            onRiwayatClick = {
                                navController.navigate("riwayat_sampah")
                            },

                            onLogoutClick = {

                                authViewModel.logout()

                                navController.navigate("login") {

                                    popUpTo("home") {
                                        inclusive = false
                                    }

                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // =========================
                    // AI SCANNER
                    // =========================
                    composable("ai_scanner") {

                        AiScannerScreen(

                            onBackClick = {
                                navController.popBackStack()
                            },

                            onGunakanHasil = { kategori ->

                                navController.navigate(
                                    "catat_sampah/$kategori"
                                )
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
                    // CATAT SAMPAH DARI AI
                    // =========================
                    composable(
                        route = "catat_sampah/{jenisSampah}",
                        arguments = listOf(
                            navArgument("jenisSampah") {
                                type = NavType.StringType
                            }
                        )
                    ) { backStackEntry ->

                        val jenisSampah =
                            backStackEntry.arguments
                                ?.getString("jenisSampah")
                                ?: ""

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

                                navController.navigate(
                                    "edit_sampah/${sampah.id}"
                                )
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

                        val sampahId =
                            backStackEntry.arguments
                                ?.getInt("sampahId")
                                ?: return@composable

                        val semuaSampah by
                        sampahViewModel.semuaSampah.collectAsState(
                            initial = emptyList()
                        )

                        val sampah =
                            semuaSampah.find {
                                it.id == sampahId
                            } ?: return@composable

                        EditSampahScreen(
                            sampah = sampah,

                            onSimpanClick = { sampahBaru ->

                                sampahViewModel.updateSampah(
                                    sampahBaru
                                )

                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}