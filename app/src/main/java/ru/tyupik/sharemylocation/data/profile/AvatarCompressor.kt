package ru.tyupik.sharemylocation.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Сжатие выбранного изображения аватара на устройстве.
 * Декодирует с сэмплингом, масштабирует до максимальной стороны [MAX_SIDE_PX]
 * и перекодирует в JPEG с качеством [JPEG_QUALITY]; результат кладёт в кэш приложения.
 */
@Singleton
class AvatarCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Сжимает изображение по [localUri] и возвращает абсолютный путь к файлу в кэше. */
    fun compressToCache(localUri: String): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(localUri)?.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }

        val options = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = openStream(localUri)?.use { stream -> BitmapFactory.decodeStream(stream, null, options) }
            ?: throw IllegalArgumentException("Не удалось декодировать изображение аватара: $localUri")

        val scaled = scaleToFit(decoded)
        val cacheFile = File(context.cacheDir, "avatar_" + System.currentTimeMillis() + ".jpg")
        cacheFile.outputStream().use { output -> scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output) }
        if (scaled !== decoded) {
            scaled.recycle()
        }
        decoded.recycle()
        return cacheFile.absolutePath
    }

    /**
     * Читает подготовленный файл в память для записи в Firestore; если JPEG крупнее
     * [RECOMPRESS_THRESHOLD_BYTES], пережимается повторно с качеством [RECOMPRESS_QUALITY].
     * Итоговый размер проверяет FirebaseBackend: превышение лимита документа вернёт AvatarTooLarge.
     */
    fun readBytes(cachedPath: String): ByteArray {
        val bytes = File(cachedPath).readBytes()
        if (bytes.size <= RECOMPRESS_THRESHOLD_BYTES) {
            return bytes
        }
        val bitmap = BitmapFactory.decodeFile(cachedPath) ?: return bytes
        val recompressed = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, RECOMPRESS_QUALITY, output)
            output.toByteArray()
        }
        bitmap.recycle()
        return recompressed
    }

    private fun openStream(localUri: String) =
        context.contentResolver.openInputStream(Uri.parse(localUri))

    /** Степень двойки для inSampleSize: после сэмплинга максимальная сторона ещё >= целевой. */
    private fun computeSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        if (width <= 0 || height <= 0) {
            return sampleSize
        }
        while (max(width, height) / (sampleSize * 2) >= MAX_SIDE_PX) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /** Масштабирует до максимальной стороны MAX_SIDE_PX с сохранением пропорций. */
    private fun scaleToFit(bitmap: Bitmap): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height)
        if (maxSide <= MAX_SIDE_PX) {
            return bitmap
        }
        val scale = MAX_SIDE_PX.toFloat() / maxSide
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private companion object {
        /** Максимальная сторона аватара после сжатия. */
        const val MAX_SIDE_PX = 1024

        /** Качество JPEG при перекодировании. */
        const val JPEG_QUALITY = 80

        /** Порог размера, после которого JPEG пережимается повторно (лимит документа Firestore — 1 МБ). */
        const val RECOMPRESS_THRESHOLD_BYTES = 700_000

        /** Качество JPEG при повторном сжатии. */
        const val RECOMPRESS_QUALITY = 50
    }
}
