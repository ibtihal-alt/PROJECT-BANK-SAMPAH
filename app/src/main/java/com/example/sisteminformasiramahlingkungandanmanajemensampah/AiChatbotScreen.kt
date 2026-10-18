package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PrimaryGreen = Color(0xFF006C4C)
private val BackgroundLight = Color(0xFFDCF0E0)
private val CardBackground = Color(0xFFFFFFFF)
private val ButterYellow = Color(0xFFF7CF68)
private val ButterYellowDark = Color(0xFF2E2405)
private val BotBubble = Color(0xFFFFFFFF)
private val UserBubble = Color(0xFFF7CF68)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatbotScreen(
    onBackClick: () -> Unit = {}
) {
    var inputMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Halo! Saya SIRAM Smart AI Assistant (Offline Mode). Asisten cerdas Bank Sampah Anda yang siap membantu 24/7 tanpa batas kuota atau koneksi internet. Ada yang ingin Anda tanyakan seputar pemilahan sampah, panduan daur ulang, atau fitur aplikasi?",
                isUser = false
            )
        )
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // DATASET LENGKAP PENGETAHUAN LOKAL (TANPA API)
    val knowledgeBase = listOf(
        // Kategori Plastik
        listOf("plastik", "botol", "gelas", "kresek", "ember", "pipa", "pvc") to
                "♻️ **Sampah Plastik**: Bersihkan dan keringkan botol atau wadah plastik sebelum disetorkan ke SIRAM. Plastik yang bersih memiliki nilai jual lebih tinggi dan lebih mudah didaur ulang menjadi bijih plastik.",

        // Kategori Kertas
        listOf("kertas", "kardus", "koran", "majalah", "hvs", "buku") to
                "📦 **Kertas & Kardus**: Lipat kardus agar tidak memakan tempat dan pastikan tidak basah atau terkena minyak. Kertas sangat bernilai tinggi dalam industri daur ulang pulp.",

        // Kategori Logam
        listOf("logam", "besi", "kaleng", "aluminium", "tembaga", "seng", "paku") to
                "⚙️ **Logam & Besi**: Kaleng minuman aluminium, potongan besi, dan tembaga adalah jenis sampah dengan nilai ekonomis tertinggi di bank sampah. Kumpulkan dan pisahkan dari material lain.",

        // Kategori Organik
        listOf("organik", "sisa makanan", "sayur", "buah", "daun", "nasi", "kompos") to
                "🌱 **Sampah Organik**: Sampah sisa makanan dan daun kering sangat ideal diolah menjadi pupuk kompos atau pakan maggot. Anda juga bisa menguburnya di pekarangan rumah untuk menyuburkan tanah.",

        // Kategori B3
        listOf("b3", "baterai", "lampu", "neon", "obat nyamuk", "kimia", "jarum", "medis") to
                "⚠️ **Limbah B3 (Bahan Berbahaya & Beracun)**: Baterai bekas, lampu neon, dan limbah kimia memerlukan penanganan khusus dan tidak boleh dibuang sembarangan karena mencemari air dan tanah.",

        // Fitur: Catat Sampah
        listOf("catat", "input", "mandiri", "menimbang") to
                "📝 **Fitur Catat Sampah**: Gunakan menu 'Catat Sampah' di dashboard untuk mencatat jenis dan estimasi berat sampah Anda secara mandiri sebelum divalidasi oleh admin.",

        // Fitur: Jemput Sampah
        listOf("jemput", "kurir", "rumah", "datang", "pickup") to
                "🚚 **Fitur Jemput Sampah**: Pilih menu 'Jemput Sampah' untuk memesan kurir lapangan SIRAM agar datang langsung ke rumah Anda. Masukkan alamat lengkap dan nomor HP yang aktif.",

        // Fitur: Tarik Saldo / Pencairan
        listOf("tarik", "saldo", "pencairan", "uang", "gopay", "ovo", "dana", "rekening", "transfer") to
                "💸 **Fitur Tarik Saldo**: Hasil penjualan sampah Anda akan menjadi saldo digital. Anda bisa mencairkannya kapan saja melalui menu 'Tarik Saldo' ke rekening bank atau dompet digital (GoPay, OVO, DANA).",

        // Fitur: Lacak GPS
        listOf("lacak", "gps", "peta", "map", "kurir", "posisi") to
                "🗺️ **Lacak Armada GPS**: Gunakan fitur peta real-time di aplikasi untuk memantau posisi kurir penjemput yang sedang dalam perjalanan menuju lokasi Anda.",

        // Sapaan
        listOf("halo", "hi", "hai", "pagi", "siang", "malam", "assalamu", "p") to
                "Halo! Ada yang bisa saya bantu terkait pemilahan sampah atau penggunaan aplikasi SIRAM hari ini? 😊",

        // Terima kasih
        listOf("terima kasih", "makasih", "thanks", "thx") to
                "Sama-sama! Senang bisa membantu Anda menjaga kelestarian lingkungan bersama SIRAM. 🌱"
    )

    fun generateSmartResponse(query: String): String {
        val q = query.lowercase()
        for ((keywords, answer) in knowledgeBase) {
            if (keywords.any { q.contains(it) }) {
                return answer
            }
        }
        return "🤖 Terima kasih atas pertanyaannya! Berdasarkan panduan bank sampah SIRAM, pastikan Anda memilah sampah dengan benar (organik, anorganik, B3) sebelum melakukan setoran atau pemesanan penjemputan. Anda juga bisa menjelajahi fitur Catat Sampah, Jemput Sampah, dan Tarik Saldo di aplikasi ini. Ada hal lain yang ingin didiskusikan?"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text("SIRAM Smart Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("100% Offline & Cerdas ⚡", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            // CHAT MESSAGES LIST
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(chatMessages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (message.isUser) 18.dp else 4.dp,
                                bottomEnd = if (message.isUser) 4.dp else 18.dp
                            ),
                            shadowElevation = 1.dp,
                            color = if (message.isUser) UserBubble else BotBubble,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = message.text,
                                fontSize = 13.sp,
                                color = if (message.isUser) ButterYellowDark else Color(0xFF1E293B),
                                modifier = Modifier.padding(14.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BotBubble
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryGreen
                                    )
                                    Text("Sedang berpikir...", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }
            }

            // INPUT BAR
            Surface(
                color = CardBackground,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = { Text("Tanya tentang sampah atau aplikasi...", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (inputMessage.isNotBlank() && !isLoading) {
                                val userText = inputMessage.trim()
                                chatMessages.add(ChatMessage(text = userText, isUser = true))
                                inputMessage = ""
                                isLoading = true

                                coroutineScope.launch {
                                    listState.animateScrollToItem(chatMessages.size - 1)
                                    // Simulasi jeda berpikir agar natural
                                    delay(600)
                                    val botReply = generateSmartResponse(userText)
                                    chatMessages.add(ChatMessage(text = botReply, isUser = false))
                                    isLoading = false
                                    listState.animateScrollToItem(chatMessages.size - 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (inputMessage.isNotBlank()) ButterYellow else Color(0xFFCBD5E1))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Kirim",
                            tint = if (inputMessage.isNotBlank()) ButterYellowDark else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
