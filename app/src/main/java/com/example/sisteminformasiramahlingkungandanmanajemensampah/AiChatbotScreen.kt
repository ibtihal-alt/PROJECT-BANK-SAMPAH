package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PrimaryGreen = Color(0xFF006C4C)
private val BackgroundLight = Color(0xFFF8FAFC)
private val CardBackground = Color(0xFFFFFFFF)
private val BotBubble = Color(0xFFE2E8F0)
private val UserBubble = Color(0xFF006C4C)

// MASUKKAN API KEY GRATIS KAMU DI SINI
private const val GEMINI_API_KEY = "DUMMY_GEMINI_KEY_REMOVED_FOR_SECURITY_REASONS_0000000"

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
                text = "Halo! Saya SIRAM AI Assistant, asisten pintar Bank Sampah Anda. Ada yang bisa saya bantu seputar pemilahan sampah, panduan daur ulang, atau penggunaan aplikasi?",
                isUser = false
            )
        )
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = GEMINI_API_KEY,
            systemInstruction = content {
                text(
                    """
                    Kamu adalah SIRAM AI Assistant, asisten virtual ramah untuk aplikasi bank sampah SIRAM.
                    Tugasmu adalah menjawab pertanyaan pengguna seputar:
                    1. Pemilahan sampah (Plastik, Kertas, Logam, Kaca, Organik, B3).
                    2. Fitur aplikasi SIRAM (Catat Sampah, Jemput Sampah ke Rumah, Tarik Saldo/Pencairan).
                    3. Edukasi lingkungan dan daur ulang.
                    Jawablah menggunakan Bahasa Indonesia yang ramah, sopan, ringkas, dan mudah dipahami.
                    """.trimIndent()
                )
            }
        )
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
                            Text("SIRAM AI Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Ditenagai Gemini AI ✨", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(chatMessages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                bottomEnd = if (msg.isUser) 4.dp else 16.dp
                            ),
                            color = if (msg.isUser) UserBubble else BotBubble,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                fontSize = 13.sp,
                                color = if (msg.isUser) Color.White else Color(0xFF1E293B),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BotBubble
                            ) {
                                Text(
                                    text = "Sedang mengetik...",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                color = CardBackground,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = { Text("Tanyakan sesuatu seputar sampah...", fontSize = 13.sp) },
                        singleLine = true,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputMessage.isNotBlank() && !isLoading) {
                                val userText = inputMessage.trim()
                                chatMessages.add(ChatMessage(text = userText, isUser = true))
                                inputMessage = ""
                                isLoading = true

                                coroutineScope.launch {
                                    if (chatMessages.isNotEmpty()) {
                                        listState.animateScrollToItem(chatMessages.size - 1)
                                    }

                                    try {
                                        val responseText = withContext(Dispatchers.IO) {
                                            val response = generativeModel.generateContent(userText)
                                            response.text
                                        }

                                        val reply: String = if (!responseText.isNullOrEmpty()) {
                                            responseText
                                        } else {
                                            "Maaf, AI tidak memberikan jawaban untuk pertanyaan tersebut."
                                        }

                                        chatMessages.add(ChatMessage(text = reply, isUser = false))
                                    } catch (e: Exception) {
                                        Log.e("SIRAM_GEMINI", "Error saat memanggil API: ${e.message}", e)

                                        val errorText: String = e.localizedMessage ?: "Terjadi kesalahan yang tidak diketahui."
                                        chatMessages.add(
                                            ChatMessage(
                                                text = "Error API: $errorText",
                                                isUser = false
                                            )
                                        )
                                    } finally {
                                        isLoading = false
                                        if (chatMessages.isNotEmpty()) {
                                            listState.animateScrollToItem(chatMessages.size - 1)
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isLoading) Color.Gray else PrimaryGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Kirim",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}