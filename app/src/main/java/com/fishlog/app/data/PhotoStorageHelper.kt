package com.fishlog.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PhotoStorageHelper(private val context: Context) {

    private val photosDir = File(context.filesDir, "catch_photos").apply {
        if (!exists()) mkdirs()
    }

    fun getNewPhotoUri(): Uri {
        val fileName = "photo_${UUID.randomUUID()}.jpg"
        val photoFile = File(photosDir, fileName)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    fun savePhoto(sourceUri: Uri): String? {
        return try {
            val fileName = "photo_${UUID.randomUUID()}.jpg"
            val destFile = File(photosDir, fileName)
            
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun savePhotoBytes(bytes: ByteArray, fileName: String): String? {
        return try {
            val destFile = File(photosDir, fileName)
            destFile.writeBytes(bytes)
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPhotoFile(uriString: String?): File? {
        if (uriString == null) return null
        return try {
            val uri = Uri.parse(uriString)
            val file = File(uri.path ?: return null)
            if (file.exists()) file else null
        } catch (e: Exception) {
            null
        }
    }

    fun deletePhoto(uriString: String?) {
        if (uriString == null) return
        try {
            val uri = Uri.parse(uriString)
            val file = File(uri.path ?: return)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

