package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.sisteminformasiramahlingkungandanmanajemensampah.data.Sampah
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGeneratorHelper {

    fun cetakStrukSampahPdf(context: Context, sampah: Sampah) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // Ukuran Halaman Struk Standar (Lebar: 400, Tinggi: 600)
        val pageInfo = PdfDocument.PageInfo.Builder(400, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // -- STYLING KANVAS PDF --
        canvas.drawColor(Color.WHITE)

        // Header Logo / Judul
        titlePaint.color = Color.rgb(0, 108, 76) // Primary Green SIRAM
        titlePaint.textSize = 22f
        titlePaint.isFakeBoldText = true
        canvas.drawText("STRUK RESMI SIRAM", 30f, 50f, titlePaint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 12f
        canvas.drawText("Sistem Informasi Ramah Lingkungan & Manajemen Sampah", 30f, 70f, paint)

        // Garis Pembatas
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 2f
        canvas.drawLine(30f, 85f, 370f, 85f, paint)

        // Informasi Transaksi
        var startY = 120f
        val labelPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 13f
        }
        val valuePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 14f
            isFakeBoldText = true
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        // Perbaikan penulisan variabel dataStruk
        val dataStruk = listOf(
            "ID Transaksi" to "#${sampah.id}",
            "Tanggal" to dateFormat.format(Date(sampah.tanggal)),
            "Jenis Sampah" to sampah.jenisSampah,
            "Berat Daur Ulang" to "${String.format("%.1f", sampah.berat)} Kg",
            "Tarif / Kg" to currencyFormat.format(sampah.hargaPerKg),
            "Status Verifikasi" to sampah.status.uppercase()
        )

        for ((label, value) in dataStruk) {
            canvas.drawText(label, 30f, startY, labelPaint)
            canvas.drawText(value, 200f, startY, valuePaint)
            startY += 30f
        }

        // Garis Pembatas Total
        canvas.drawLine(30f, startY + 10f, 370f, startY + 10f, paint)
        startY += 40f

        // Total Nominal Saldo
        val totalLabel = Paint().apply {
            color = Color.rgb(0, 108, 76)
            textSize = 15f
            isFakeBoldText = true
        }
        val totalValue = Paint().apply {
            color = Color.rgb(0, 108, 76)
            textSize = 20f
            isFakeBoldText = true
        }

        canvas.drawText("TOTAL SALDO DIPEROLEH", 30f, startY, totalLabel)
        startY += 25f
        canvas.drawText(currencyFormat.format(sampah.totalNilai), 30f, startY, totalValue)

        // Footer Catatan
        startY += 60f
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Terima kasih telah berpartisipasi menjaga kelestarian lingkungan.", 200f, startY, footerPaint)
        canvas.drawText("Dokumen ini dicetak secara digital dan sah oleh SIRAM.", 200f, startY + 15f, footerPaint)

        pdfDocument.finishPage(page)

        // Penyimpanan File ke MediaStore (Folder Download Android)
        val fileName = "Struk_SIRAM_${sampah.id}_${System.currentTimeMillis()}.pdf"

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                }
            } else {
                val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(targetDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
            }

            pdfDocument.close()
            Toast.makeText(context, "✅ Struk berhasil diunduh ke folder Downloads!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            pdfDocument.close()
            Toast.makeText(context, "❌ Gagal mencetak struk: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}