package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PrimaryGreen = Color(0xFF006C4C)
private val BackgroundLight = Color(0xFFDCF0E0)
private val CardBackground = Color(0xFFFFFFFF)
private val ButterYellow = Color(0xFFF7CF68)
private val ButterYellowDark = Color(0xFF2E2405)
private val RedDanger = Color(0xFFDC2626)

data class BroadcastModel(
    val id: String = "",
    val judul: String = "",
    val pesan: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var judul by remember { mutableStateOf("") }
    var pesan by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var broadcastList by remember { mutableStateOf<List<BroadcastModel>>(emptyList()) }

    DisposableEffect(Unit) {
        val listener = firestore.collection("broadcasts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    broadcastList = snapshot.documents.map { doc ->
                        BroadcastModel(
                            id = doc.id,
                            judul = doc.getString("judul") ?: "",
                            pesan = doc.getString("pesan") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }
                }
            }
        onDispose { listener.remove() }
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Broadcast Pengumuman Sistem", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(start = 12.dp).size(40.dp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF0F172A))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(26.dp))
                            Column {
                                Text("Kirim Pesan Siaran Langsung", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                Text("Pesan akan langsung tampil di dashboard semua nasabah & kurir.", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }

                        OutlinedTextField(
                            value = judul,
                            onValueChange = { judul = it },
                            label = { Text("Judul Pengumuman") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = pesan,
                            onValueChange = { pesan = it },
                            label = { Text("Isi Pesan / Informasi Penting") },
                            minLines = 4,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (judul.isNotBlank() && pesan.isNotBlank()) {
                                    isLoading = true
                                    val broadcastData = hashMapOf(
                                        "judul" to judul.trim(),
                                        "pesan" to pesan.trim(),
                                        "timestamp" to System.currentTimeMillis(),
                                        "sender" to (auth.currentUser?.email ?: "Super Admin")
                                    )
                                    firestore.collection("broadcasts").add(broadcastData)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            Toast.makeText(context, "✅ Broadcast berhasil dikirim!", Toast.LENGTH_SHORT).show()
                                            judul = ""
                                            pesan = ""
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            Toast.makeText(context, "Gagal mengirim broadcast: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    Toast.makeText(context, "Judul dan pesan wajib diisi!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButterYellow,
                                contentColor = ButterYellowDark
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("KIRIM BROADCAST SEKARANG", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Daftar Siaran Aktif (${broadcastList.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
            }

            if (broadcastList.isEmpty()) {
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada siaran broadcast aktif.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            } else {
                items(broadcastList, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.judul, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = item.pesan, fontSize = 12.sp, color = Color(0xFF334155))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = dateFormat.format(Date(item.timestamp)), fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }

                            IconButton(
                                onClick = {
                                    firestore.collection("broadcasts").document(item.id).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "🗑️ Broadcast dihapus", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Gagal menghapus broadcast", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RedDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}
