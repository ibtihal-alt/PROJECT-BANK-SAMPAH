package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState =
        MutableStateFlow<AuthState>(AuthState.Idle)

    val authState: StateFlow<AuthState> =
        _authState.asStateFlow()

    fun logout() {
        auth.signOut()
        resetState()
    }

    // =========================
    // REGISTER USER + FIRESTORE ROLE
    // =========================
    fun register(
        email: String,
        password: String,
        nama: String = ""
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
                // 1. Buat Akun Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(
                    email.trim(),
                    password
                ).await()

                val uid = authResult.user?.uid ?: ""

                // 2. Simpan Data User & Role Default "nasabah" ke Firestore
                if (uid.isNotEmpty()) {
                    val userData = hashMapOf(
                        "uid" to uid,
                        "email" to email.trim(),
                        "nama" to nama.ifBlank { email.substringBefore("@") },
                        "role" to "nasabah", // Default role
                        "createdAt" to System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(uid)
                        .set(userData)
                        .await()
                }

                // 3. Logout agar user melakukan login
                auth.signOut()

                _authState.value = AuthState.RegisterSuccess

            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    e.message ?: "Registrasi gagal"
                )
            }
        }
    }

    // =========================
    // LOGIN
    // =========================
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

                _authState.value = AuthState.LoginSuccess

            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    "Email atau password salah"
                )
            }
        }
    }

    // =========================
    // CEK ROLE USER AKTIF
    // =========================
    fun fetchUserRole(onResult: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult("nasabah")
            return
        }

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val role = document.getString("role") ?: "nasabah"
                onResult(role)
            }
            .addOnFailureListener {
                onResult("nasabah")
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
    data class Error(val message: String) : AuthState()
}