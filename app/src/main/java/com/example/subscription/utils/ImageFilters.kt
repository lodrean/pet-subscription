package com.example.subscription.utils

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas

object ImageFilters {

    fun applyGrayscale(bitmap: Bitmap): Bitmap {
        return applyColorMatrix(bitmap, ColorMatrix().apply {
            setSaturation(0f)
        })
    }

    fun applySepia(bitmap: Bitmap): Bitmap {
        val matrix = ColorMatrix().apply {
            set(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        return applyColorMatrix(bitmap, matrix)
    }

    fun applyBrightness(bitmap: Bitmap, value: Float): Bitmap {
        val matrix = ColorMatrix().apply {
            set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, value,
                    0f, 1f, 0f, 0f, value,
                    0f, 0f, 1f, 0f, value,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        return applyColorMatrix(bitmap, matrix)
    }

    fun applyContrast(bitmap: Bitmap, value: Float): Bitmap {
        val scale = value
        val translate = (-.5f * scale + .5f) * 255f
        val matrix = ColorMatrix().apply {
            set(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        return applyColorMatrix(bitmap, matrix)
    }

    private fun applyColorMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        Canvas(result).drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
