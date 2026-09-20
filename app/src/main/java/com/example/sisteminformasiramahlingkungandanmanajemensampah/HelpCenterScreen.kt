package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.SupportAgent
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

private val PrimaryGreen = Color(0xFF006C4C)
private val BackgroundLight = Color(0xFFF8FAFC)
private val CardBackground = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val faqs = listOf(
        "Bagaimana cara menyetor sampah mandiri?" to "Buka menu 'Catat Sampah' di dashboard Anda, pilih jenis sampah, masukkan berat estimasi, lalu simpan. Admin/Super Admin akan memvalidasi setoran tersebut.",
        "Bagaimana cara memesan penjemputan ke rumah?" to "Buka menu 'Jemput Sampah', masukkan alamat lengkap, nomor HP, dan estimasi berat sampah. Kurir lapangan akan ditugaskan untuk menjemput ke lokasi Anda.",
        "Kapan saldo hasil penjualan sampah dapat dicairkan?" to "Saldo akan masuk ke dompet digital Anda setelah kurir selesai menimbang sampah di lokasi dan disetujui oleh Super Admin. Anda bisa langsung melakukan 'Tarik Saldo'.",
        "Berapa minimal penarikan saldo?" to "Minimal penarikan saldo adalah Rp 10.000 dengan pilihan metode transfer ke Rekening Bank, GoPay, OVO, atau DANA."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pusat Bantuan & FAQ", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text("Butuh Bantuan Langsung?", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                Text("Tim layanan pelanggan SIRAM siap membantu Anda.", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }

                        Button(
                            onClick = {
                                val url = "https://api.whatsapp.com/send?phone=6281222616990&text=${Uri.encode("Halo Admin SIRAM, saya ingin bertanya...")}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HUBUNGI CS VIA WHATSAPP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Pertanyaan Umum (FAQ)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(faqs) { (question, answer) ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                            Text(
                                text = question,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Text(
                            text = answer,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
