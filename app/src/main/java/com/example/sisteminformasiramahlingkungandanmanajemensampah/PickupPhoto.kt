package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.PickupTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

// Tahap foto penjemputan (bagian dari ID dokumen pickup_photos)
const val FOTO_NASABAH = "nasabah"
const val FOTO_AMBIL = "ambil"
const val FOTO_SELESAI = "selesai"

val TAHAP_FOTO_NASABAH = listOf(FOTO_NASABAH to "Foto dari Nasabah")
val TAHAP_FOTO_SEMUA = listOf(
    FOTO_NASABAH to "Foto dari Nasabah",
    FOTO_AMBIL to "Foto Ambil Sampah (Kurir)",
    FOTO_SELESAI to "Foto Timbangan (Kurir)"
)

private const val MAX_SIDE_PX = 1280
private const val MAX_JPEG_BYTES = 700_000

// Foto kamera dikecilkan agar muat di satu dokumen Firestore (maks 1 MB) dan diputar sesuai EXIF
fun compressPhoto(context: Context, uri: Uri): ByteArray {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2

    val decoded = resolver.openInputStream(uri).use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: throw IllegalStateException("Foto tidak bisa dibaca")

    val rotation = resolver.openInputStream(uri)?.use { input ->
        when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f

    val scale = minOf(1f, MAX_SIDE_PX.toFloat() / maxOf(decoded.width, decoded.height))
    val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
    val bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

    var quality = 80
    while (true) {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (out.size() <= MAX_JPEG_BYTES || quality <= 30) return out.toByteArray()
        quality -= 10
    }
}

// Kamera langsung tanpa pilihan galeri (bukti foto asli, seperti kurir ekspedisi); hasil sudah dikompres
@Composable
fun rememberCameraCapture(onPhoto: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingUri
        if (success && uri != null) {
            scope.launch {
                try {
                    onPhoto(withContext(Dispatchers.IO) { compressPhoto(context, uri) })
                } catch (e: Exception) {
                    Toast.makeText(context, "Foto gagal diproses: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    return {
        val dir = File(context.cacheDir, "foto_sampah").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // foto lama sudah dikompres ke memori, file mentah tidak dipakai lagi
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(dir, "foto_${System.currentTimeMillis()}.jpg"))
        pendingUri = uri
        try {
            launcher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Aplikasi kamera tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
}

// Foto tahap tertentu, real time: langsung berubah saat foto diunggah dari HP lain
@Composable
fun rememberPickupPhoto(firebaseId: String, tahap: String): ByteArray? {
    var bytes by remember(firebaseId, tahap) { mutableStateOf<ByteArray?>(null) }
    DisposableEffect(firebaseId, tahap) {
        val registration = if (firebaseId.isBlank()) null
        else FirestorePickupRepository().listenFoto(firebaseId, tahap) { bytes = it }
        onDispose { registration?.remove() }
    }
    return bytes
}

// Foto-foto penjemputan untuk admin/super admin; foto muncul real time begitu diunggah
@Composable
fun PickupPhotosDialog(pickup: PickupTransaction, tahapList: List<Pair<String, String>>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foto Penjemputan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tahapList.forEach { (tahap, label) ->
                    val foto = rememberPickupPhoto(pickup.firebaseId, tahap)
                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (foto != null) {
                        AsyncImage(
                            model = foto,
                            contentDescription = label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text("Belum ada foto", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("TUTUP") }
        }
    )
}

// Tombol ambil + unggah foto untuk satu tahap; menampilkan thumbnail bila foto sudah ada
@Composable
fun FotoTahapButton(pickup: PickupTransaction, tahap: String, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val foto = rememberPickupPhoto(pickup.firebaseId, tahap)
    var uploading by remember { mutableStateOf(false) }
    val takePhoto = rememberCameraCapture { bytes ->
        uploading = true
        scope.launch {
            try {
                FirestorePickupRepository().uploadFoto(pickup.firebaseId, tahap, bytes)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal mengunggah foto: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                uploading = false
            }
        }
    }

    OutlinedButton(
        onClick = takePhoto,
        enabled = !uploading && pickup.firebaseId.isNotBlank(),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (foto != null) {
            AsyncImage(
                model = foto,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
            )
        } else {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = when {
                uploading -> "Mengunggah..."
                foto != null -> "✅ $label (foto ulang)"
                else -> "$label (wajib)"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
