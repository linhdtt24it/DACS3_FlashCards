package com.example.flashcards.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // Giới hạn kích thước ảnh (500x500) để nén dung lượng, tránh lỗi Firestore
            val maxWidth = 500
            val maxHeight = 500
            var width = originalBitmap.width
            var height = originalBitmap.height

            val resizedBitmap = if (width > maxWidth || height > maxHeight) {
                val ratioBitmap = width.toFloat() / height.toFloat()
                val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
                var finalWidth = maxWidth
                var finalHeight = maxHeight
                if (ratioMax > ratioBitmap) {
                    finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
                } else {
                    finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
                }
                Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImageModel(imageUrl: String?): Any? {
        if (imageUrl == null) return null
        return if (imageUrl.startsWith("data:image")) {
            try {
                val base64 = imageUrl.substringAfter("base64,")
                Base64.decode(base64, Base64.DEFAULT)
            } catch (e: Exception) {
                imageUrl
            }
        } else {
            imageUrl
        }
    }
}