package com.example.projectpa

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object PredictionManager {
    private val bool = true
    private val url =if (bool) "https://keshinryan-cloudfinalproject.hf.space/predict/" else "https://final-project-vercel-delta.vercel.app/predict"
    suspend fun predict(context: Context, imageUri: Uri): Map<String, Pair<String, Float>> {
        val isEdge = !isOnline(context)

        // Simpan baseline CPU sebelum prediksi (agar delta bisa dihitung nanti)
        getCpuUsagePercent()

        // Mulai hitung waktu latency
        val startTime = System.nanoTime()
        val result = if (isEdge) predictViaTFLite(context, imageUri) else predictViaCloud(context, imageUri)
        val latencyMs = (System.nanoTime() - startTime) / 1_000_000
        Log.d("LatencyTest", "${if (isEdge) "Edge" else "Cloud"} AI latency: $latencyMs ms")

        // RAM bisa diambil langsung
        val ram = getMemoryUsage(context)

        // Delay sebentar supaya delta CPU bisa dihitung
        delay(1000)

        // Hitung CPU usage berdasarkan delta dari baseline tadi
        val cpu = getCpuUsagePercent()
        Log.d("PerfMonitor", "RAM: $ram KB, CPU: ${"%.2f".format(cpu)}%")

        return result
    }

    private fun getMemoryUsage(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pid = android.os.Process.myPid()
        val memoryInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))[0]
        val memoryUsageKb = memoryInfo.totalPss // in kB
        return memoryUsageKb
    }


    private var lastCpuTime: Long? = null
    private var lastWallTime: Long? = null

    private fun getCpuUsagePercent(): Float {
        val currentCpuTime = android.os.Process.getElapsedCpuTime()
        val currentWallTime = SystemClock.elapsedRealtime()

        val lastCpu = lastCpuTime
        val lastWall = lastWallTime

        // Jika pertama kali, simpan dulu dan kembalikan -1 (menandakan belum tersedia)
        if (lastCpu == null || lastWall == null) {
            lastCpuTime = currentCpuTime
            lastWallTime = currentWallTime
            return -1f
        }

        val cpuDelta = currentCpuTime - lastCpu
        val wallDelta = currentWallTime - lastWall

        lastCpuTime = currentCpuTime
        lastWallTime = currentWallTime

        if (wallDelta == 0L) return 0f

        val usage = (cpuDelta.toFloat() / wallDelta.toFloat()) * 100f
        return usage.coerceIn(0f, 100f)
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun predictViaCloud(context: Context, uri: Uri): Map<String, Pair<String, Float>> {
        val compressedBytes = compressImageLikeWhatsApp(context, uri)

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "file",
                filename = "image.jpg",
                body = compressedBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (!response.isSuccessful) {
            throw Exception("Cloud API call failed: ${response.code}")
        }

        val responseBody = response.body?.string()
            ?: throw Exception("Response body is null")

        val json = JSONObject(responseBody)
        val result = mutableMapOf<String, Pair<String, Float>>()

        val keys = listOf("kelas", "ordo", "famili", "genus", "spesies")
        for (key in keys) {
            val obj = json.getJSONObject(key)
            val label = obj.getString("label")
            val confidence = obj.getDouble("confidence").toFloat()
            result[key] = label to confidence
        }
        println("Result : $result")
        return result
    }

    private fun predictViaTFLite(context: Context, uri: Uri): Map<String, Pair<String, Float>> {
        val tflite = Interpreter(loadModelFile(context, "D3(4HL+0.2D).tflite"))
        val labels = loadJsonLabels(context, "label_encodings2.json")
        val input = preprocessImageForEdge(context, uri)

        val outputIndexToKey = mapOf(
            0 to "famili",
            1 to "spesies",
            2 to "ordo",
            3 to "genus",
            4 to "kelas"
        )
        val outputMap = mutableMapOf<Int, Any>()
        for ((index, key) in outputIndexToKey) {
            val classCount = labels[key]?.size ?: error("Label not found for $key")
            outputMap[index] = Array(1) { FloatArray(classCount) }
        }

        tflite.runForMultipleInputsOutputs(arrayOf(input), outputMap)

        val result = mutableMapOf<String, Pair<String, Float>>()
        for ((index, key) in outputIndexToKey) {
            val probs = (outputMap[index] as Array<FloatArray>)[0]
            val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
            val confidence = probs[maxIdx]
            val label = labels[key]?.entries?.find { it.value == maxIdx }?.key ?: "Unknown"
            result[key] = label to confidence
        }
        println("Result : $result")
        return result
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun loadJsonLabels(context: Context, filename: String): Map<String, Map<String, Int>> {
        val jsonStr = context.assets.open(filename).bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
        return Gson().fromJson(jsonStr, type)
    }

    private fun preprocessImageForEdge(context: Context, imageUri: Uri): Array<Array<Array<FloatArray>>> {
        // Get compressed image bytes
        val compressedBytes = compressImageLikeWhatsApp(context, imageUri)
        val bitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

        // Resize with bilinear filtering to match Python Pillow default (BILINEAR)
        val resized = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resized)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = Rect(0, 0, 256, 256)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        bitmap.recycle()

        // Normalize to [0, 1] like Python (img_to_array / 255.0)
        val result = Array(1) { Array(256) { Array(256) { FloatArray(3) } } }
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                val pixel = resized.getPixel(x, y)
                result[0][y][x][0] = Color.red(pixel) / 255.0f
                result[0][y][x][1] = Color.green(pixel) / 255.0f
                result[0][y][x][2] = Color.blue(pixel) / 255.0f
            }
        }
        resized.recycle()  // Free memory
        return result
    }

    fun compressImageLikeWhatsApp(context: Context, imageUri: Uri): ByteArray {
        val stream1 = context.contentResolver.openInputStream(imageUri) ?: error("Cannot load image")
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream1, null, options)
        stream1.close()

        val maxDim = 1280f
        val (orgW, orgH) = options.outWidth to options.outHeight
        var targetW = orgW.toFloat()
        var targetH = orgH.toFloat()
        val imgRatio = targetW / targetH
        val maxRatio = maxDim / maxDim
        if (targetH > maxDim || targetW > maxDim) {
            if (imgRatio < maxRatio) {
                targetH = maxDim
                targetW = imgRatio * maxDim
            } else if (imgRatio > maxRatio) {
                targetW = maxDim
                targetH = maxDim / imgRatio
            } else {
                targetW = maxDim
                targetH = maxDim
            }
        }

        options.inSampleSize = calculateInSampleSize(options, targetW.toInt(), targetH.toInt())
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        val stream2 = context.contentResolver.openInputStream(imageUri) ?: error("Cannot load second pass")
        val sampled = BitmapFactory.decodeStream(stream2, null, options)
        stream2.close()

        val scaled = Bitmap.createBitmap(targetW.toInt(), targetH.toInt(), Bitmap.Config.RGB_565)
        val ratioX = targetW / options.outWidth
        val ratioY = targetH / options.outHeight
        val canvas = Canvas(scaled)
        canvas.setMatrix(Matrix().apply { setScale(ratioX, ratioY) })
        if (sampled != null) {
            canvas.drawBitmap(sampled, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
            sampled.recycle()
        }

        val stream3 = context.contentResolver.openInputStream(imageUri) ?: error("Cannot read EXIF")
        val exif = ExifInterface(stream3)
        stream3.close()
        val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        val rotated = if (rotation != 0) {
            Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height,
                Matrix().apply { postRotate(rotation.toFloat()) }, true).also {
                scaled.recycle()
            }
        } else scaled

        val baos = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        rotated.recycle()
        return baos.toByteArray()
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, reqW: Int, reqH: Int
    ): Int {
        val (h, w) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (h > reqH || w > reqW) {
            val halfH = h / 2
            val halfW = w / 2
            while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    private fun adjustBrightness(bitmap: Bitmap, brightness: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }


}