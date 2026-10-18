package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrimaryGreen = Color(0xFF006C4C)
private val BackgroundLight = Color(0xFFDCF0E0)
private val CardBackground = Color(0xFFFFFFFF)

data class EdukasiArticle(
    val title: String,
    val category: String,
    val summary: String,
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoEducationScreen(
    onBackClick: () -> Unit = {}
) {
    val articles = listOf(
        EdukasiArticle(
            title = "Panduan Pemilahan Sampah Rumah Tangga",
            category = "Piramida Sampah",
            summary = "Cara membedakan sampah organik, anorganik, dan B3 dari sumbernya.",
            content = "Pemilahan sampah dimulai dari dapur Anda. Pisahkan sampah sisa makanan (organik) untuk dijadikan kompos. Botol dan kardus (anorganik) dikeringkan agar bernilai jual di Bank Sampah. Hindari mencampur baterai atau lampu bekas dengan sampah biasa."
        ),
        EdukasiArticle(
            title = "Teknik Pembuatan Kompos Mandiri di Rumah",
            category = "Organik",
            summary = "Manfaatkan sampah sisa sayur dan buah menjadi pupuk tanaman subur.",
            content = "Gunakan wadah tertutup, masukkan lapisan sisa organik selang-seling dengan daun kering atau sekam. Tambahkan cairan penawar bau atau EM4. Dalam 3-4 minggu, kompos siap digunakan untuk menyuburkan tanaman hias."
        ),
        EdukasiArticle(
            title = "Nilai Ekonomis Daur Ulang Plastik & Logam",
            category = "Ekonomi Sirkular",
            summary = "Bagaimana limbah botol plastik dan kaleng bertransformasi menjadi rupiah.",
            content = "Botol plastik PET yang bersih dan tanpa label bernilai jauh lebih tinggi di pasaran daur ulang. Dengan menyetorkannya ke SIRAM, Anda tidak hanya menyelamatkan bumi dari polusi mikroplastik, tetapi juga mengumpulkan tabungan saldo."
        ),
        EdukasiArticle(
            title = "Bahaya Limbah B3 Rumah Tangga",
            category = "Keselamatan & Lingkungan",
            summary = "Penanganan khusus baterai bekas, obat kedaluwarsa, dan neon.",
            content = "Limbah B3 mengandung zat beracun seperti merkuri dan timbal. Jangan dibakar atau dibuang ke tempat sampah umum. Kumpulkan dan serahkan pada fasilitas khusus pengolahan limbah B3."
        )
    )

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Edukasi & Tips Daur Ulang", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A)) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Eco, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("Green Earth Initiative", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            Text("Kenali cara terbaik menjaga kelestarian lingkungan bersama SIRAM.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            }

            items(articles) { article ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PrimaryGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = article.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = article.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = article.summary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Text(
                            text = article.content,
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
