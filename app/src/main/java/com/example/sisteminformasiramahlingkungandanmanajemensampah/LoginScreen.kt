package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onRegisterClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {

        if (authState is AuthState.LoginSuccess) {

            authViewModel.resetState()

            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // =========================
        // LOGO
        // =========================
        Text(
            text = "♻️",
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =========================
        // JUDUL
        // =========================
        Text(
            text = "Selamat Datang",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Masuk untuk melanjutkan pengelolaan sampahmu",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        // =========================
        // EMAIL
        // =========================
        SiramTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = "Email",
            placeholder = "Masukkan alamat email",
            keyboardType = KeyboardType.Email
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =========================
        // PASSWORD
        // =========================
        SiramTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = "Password",
            placeholder = "Masukkan password",
            keyboardType = KeyboardType.Password,
            isPassword = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // =========================
        // PESAN ERROR
        // =========================
        if (authState is AuthState.Error) {

            Text(
                text = (authState as AuthState.Error).message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        // =========================
        // TOMBOL LOGIN
        // =========================
        Button(
            onClick = {

                authViewModel.login(
                    email = email,
                    password = password
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = authState !is AuthState.Loading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    MaterialTheme.colorScheme.primary
            )
        ) {

            Text(
                text =
                    if (authState is AuthState.Loading) {
                        "MEMPROSES..."
                    } else {
                        "MASUK"
                    },
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // =========================
        // REGISTER
        // =========================
        Text(
            text = "Belum memiliki akun?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TextButton(
            onClick = {

                authViewModel.resetState()

                onRegisterClick()
            }
        ) {

            Text(
                text = "Daftar sekarang",
                fontWeight = FontWeight.Bold
            )
        }
    }
}