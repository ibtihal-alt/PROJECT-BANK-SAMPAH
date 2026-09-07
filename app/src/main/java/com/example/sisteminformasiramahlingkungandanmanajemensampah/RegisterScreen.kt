package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit = {}
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var konfirmasiPassword by remember {
        mutableStateOf("")
    }

    var localError by remember {
        mutableStateOf<String?>(null)
    }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {

        if (authState is AuthState.RegisterSuccess) {

            authViewModel.resetState()

            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp
            ),
        verticalArrangement = Arrangement.Center
    ) {

        // =========================
        // HEADER
        // =========================
        Text(
            text = "Buat Akun",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Daftar untuk mulai mencatat dan mengelola sampah dengan lebih mudah.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =========================
        // FORM
        // =========================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {

                Text(
                    text = "Informasi Akun",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                SiramTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localError = null
                    },
                    label = "Email",
                    placeholder = "Masukkan alamat email",
                    keyboardType = KeyboardType.Email
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                SiramTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                    },
                    label = "Password",
                    placeholder = "Minimal 6 karakter",
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                SiramTextField(
                    value = konfirmasiPassword,
                    onValueChange = {
                        konfirmasiPassword = it
                        localError = null
                    },
                    label = "Konfirmasi Password",
                    placeholder = "Masukkan ulang password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                // =========================
                // ERROR LOKAL
                // =========================
                if (localError != null) {

                    Text(
                        text = localError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }

                // =========================
                // ERROR FIREBASE
                // =========================
                if (authState is AuthState.Error) {

                    Text(
                        text =
                            (authState as AuthState.Error).message,
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.error
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }

                // =========================
                // REGISTER BUTTON
                // =========================
                Button(
                    onClick = {

                        when {

                            email.isBlank() -> {
                                localError =
                                    "Email wajib diisi"
                            }

                            password.isBlank() -> {
                                localError =
                                    "Password wajib diisi"
                            }

                            password.length < 6 -> {
                                localError =
                                    "Password minimal 6 karakter"
                            }

                            password != konfirmasiPassword -> {
                                localError =
                                    "Konfirmasi password tidak sama"
                            }

                            else -> {

                                localError = null

                                authViewModel.register(
                                    email = email,
                                    password = password
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled =
                        authState !is AuthState.Loading
                ) {

                    Text(
                        text =
                            if (authState is AuthState.Loading) {
                                "MEMBUAT AKUN..."
                            } else {
                                "BUAT AKUN"
                            }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =========================
        // LOGIN
        // =========================
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Sudah memiliki akun?",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = {

                    authViewModel.resetState()

                    onRegisterSuccess()
                }
            ) {

                Text(
                    text = "Masuk ke akun"
                )
            }
        }
    }
}