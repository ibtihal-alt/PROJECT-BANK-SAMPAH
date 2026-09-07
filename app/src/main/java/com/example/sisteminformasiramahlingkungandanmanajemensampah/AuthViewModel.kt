package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState =
        MutableStateFlow<AuthState>(AuthState.Idle)

    val authState: StateFlow<AuthState> =
        _authState.asStateFlow()
    fun logout() {

        auth.signOut()

        resetState()
    }
    fun register(
        email: String,
        password: String
    ) {

        if (email.isBlank() || password.isBlank()) {

            _authState.value =
                AuthState.Error("Email dan password wajib diisi")

            return
        }

        if (password.length < 6) {

            _authState.value =
                AuthState.Error("Password minimal 6 karakter")

            return
        }

        viewModelScope.launch {

            _authState.value = AuthState.Loading

            try {

                auth.createUserWithEmailAndPassword(
                    email.trim(),
                    password
                ).await()

                // Setelah berhasil mendaftar,
                // pengguna logout agar wajib login kembali
                auth.signOut()

                _authState.value =
                    AuthState.RegisterSuccess

            } catch (e: Exception) {

                _authState.value =
                    AuthState.Error(
                        e.message ?: "Registrasi gagal"
                    )
            }
        }
    }

    fun login(
        email: String,
        password: String
    ) {

        if (email.isBlank() || password.isBlank()) {

            _authState.value =
                AuthState.Error("Email dan password wajib diisi")

            return
        }

        viewModelScope.launch {

            _authState.value = AuthState.Loading

            try {

                auth.signInWithEmailAndPassword(
                    email.trim(),
                    password
                ).await()

                _authState.value =
                    AuthState.LoginSuccess

            } catch (e: Exception) {

                _authState.value =
                    AuthState.Error(
                        "Email atau password salah"
                    )
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {

    data object Idle : AuthState()

    data object Loading : AuthState()

    data object RegisterSuccess : AuthState()

    data object LoginSuccess : AuthState()

    data class Error(
        val message: String
    ) : AuthState()
}