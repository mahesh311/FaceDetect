package com.mahesh.facedetection.presentation.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream

class ImageUtils {

    private val tag = this::class.java.simpleName

    fun cropFaceFromImage(
        imageFile: File,
        faceRectFromPreview: Rect,
        previewSize: Size = Size(720, 1280),
        isFrontCamera: Boolean = true,
        context: Context,
        paddingPercent: Float = 0.5f // 20% extra area around face
    ): File? {
        return try {
            Log.d(tag, "Started cropping $faceRectFromPreview")

            val exif = ExifInterface(imageFile.absolutePath)
            val rotationDegrees = when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            var bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null

            // Apply rotation and mirroring
            val matrix = Matrix().apply {
                if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
                if (isFrontCamera) postScale(-1f, 1f)
            }

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            val imageSize = Size(bitmap.width, bitmap.height)
            val mappedRect =
                mapFaceRectToImage(faceRectFromPreview, previewSize, imageSize, isFrontCamera)

            // Apply padding
            val extraWidth = (mappedRect.width() * paddingPercent).toInt()
            val extraHeight = (mappedRect.height() * (paddingPercent + 0.1)).toInt()

            val paddedRect = Rect(
                (mappedRect.left - extraWidth).coerceAtLeast(0),
                (mappedRect.top - extraHeight).coerceAtLeast(0),
                (mappedRect.right + extraWidth).coerceAtMost(bitmap.width),
                (mappedRect.bottom + extraHeight).coerceAtMost(bitmap.height)
            )

            // Sanity check
            if (paddedRect.width() < 50 || paddedRect.height() < 50) {
                Log.w(tag, "Face too small or invalid rect: $paddedRect")
                return null
            }

            val cropped = Bitmap.createBitmap(
                bitmap,
                paddedRect.left,
                paddedRect.top,
                paddedRect.width(),
                paddedRect.height()
            )

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                imageFile.delete()
//                return saveBitmapToDownloads(context, cropped, "crop_${System.currentTimeMillis()}.jpg")
//            } else {
                FileOutputStream(imageFile).use {
                    cropped.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
//            }
            Log.d(tag, "Face cropped and saved successfully.")

            return imageFile
        } catch (e: Exception) {
            Log.e(tag, "Error cropping face: ", e)
            null
        }
    }

    fun mapFaceRectToImage(
        faceRect: Rect,
        previewSize: Size,
        imageSize: Size,
        isFrontCamera: Boolean
    ): Rect {
        try {
            val previewRatio = previewSize.width.toFloat() / previewSize.height
            val imageRatio = imageSize.width.toFloat() / imageSize.height

            val scale: Float
            val dx: Float
            val dy: Float

            val previewWidth: Float
            val previewHeight: Float

            if (imageRatio > previewRatio) {
                // Image is wider → letterboxing vertically
                scale = imageSize.height.toFloat() / previewSize.height
                previewWidth = previewSize.width * scale
                dx = (imageSize.width - previewWidth) / 2
                dy = 0f
            } else {
                // Image is taller → pillarboxing horizontally
                scale = imageSize.width.toFloat() / previewSize.width
                previewHeight = previewSize.height * scale
                dx = 0f
                dy = (imageSize.height - previewHeight) / 2
            }

            val left = (faceRect.left * scale + dx).toInt()
            val top = (faceRect.top * scale + dy).toInt()
            val right = (faceRect.right * scale + dx).toInt()
            val bottom = (faceRect.bottom * scale + dy).toInt()

            val mapped = Rect(left, top, right, bottom)

            if (isFrontCamera) {
                // Mirror across vertical center
                val mirroredLeft = imageSize.width - mapped.right
                val mirroredRight = imageSize.width - mapped.left
                return Rect(mirroredLeft, mapped.top, mirroredRight, mapped.bottom)
            }
            return mapped
        } catch (e: Exception) {
            Log.e(tag, "mapFaceRectToImage: Exception in mapping face rect to image", e)
            return faceRect
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveBitmapToDownloads(context: Context, bitmap: Bitmap, filename: String): File? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val itemUri = resolver.insert(collection, contentValues)

        itemUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            return File(uri.toString())
        }

        return null
    }
}