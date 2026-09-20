package com.example.sisteminformasiramahlingkungandanmanajemensampah

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Sinkronisasi data offline lokal ke cloud (Firestore)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
