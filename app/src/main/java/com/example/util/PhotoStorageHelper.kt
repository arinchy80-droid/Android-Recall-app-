package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PhotoStorageHelper {
    private const val TAG = "PhotoStorageHelper"

    /**
     * Get or create dedicated directory inside internal storage for permanent memory photos.
     */
    fun getPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, "memory_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Saves a captured or generated Bitmap to persistent internal storage.
     * Returns the absolute path string of the saved file.
     */
    fun saveBitmap(context: Context, bitmap: Bitmap): String {
        val dir = getPhotosDirectory(context)
        val filename = "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val file = File(dir, filename)

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }
            Log.d(TAG, "Successfully saved photo bitmap to: ${file.absolutePath} (${file.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save photo bitmap: ${e.message}", e)
        }
        return file.absolutePath
    }

    /**
     * Reads an image Uri (from Photo Picker / Gallery), decodes a safe high-resolution Bitmap,
     * and saves it to internal app storage so it persists forever.
     * Returns Pair(absolutePath, bitmap) or null on failure.
     */
    fun saveUri(context: Context, uri: Uri): Pair<String, Bitmap>? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    // Prevent OOM for huge 108MP camera photos by downsampling if needed
                    val maxDimension = 2048
                    val width = info.size.width
                    val height = info.size.height
                    if (width > maxDimension || height > maxDimension) {
                        val sampleSize = maxOf(width / maxDimension, height / maxDimension)
                        decoder.setTargetSampleSize(maxOf(1, sampleSize))
                    }
                    decoder.isMutableRequired = true
                }
            } else {
                val input = context.contentResolver.openInputStream(uri) ?: return null
                val rawBytes = input.use { it.readBytes() }
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
                var sampleSize = 1
                while (boundsOpts.outWidth / sampleSize > 2048 || boundsOpts.outHeight / sampleSize > 2048) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
            }

            if (bitmap != null) {
                val path = saveBitmap(context, bitmap)
                Pair(path, bitmap)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load and save uri $uri: ${e.message}", e)
            null
        }
    }

    /**
     * Deletes a stored photo file if it exists.
     */
    fun deletePhotoFile(localUri: String?) {
        if (localUri.isNullOrBlank()) return
        try {
            val file = File(localUri)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete photo file: ${e.message}")
        }
    }
}
