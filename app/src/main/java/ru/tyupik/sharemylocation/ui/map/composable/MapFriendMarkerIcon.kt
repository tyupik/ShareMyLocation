package ru.tyupik.sharemylocation.ui.map.composable

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.tyupik.sharemylocation.ui.theme.MARKER_NEON_COLORS

/**
 * Кэш иконок маркеров: ключ — композитный «uid|путь файла аватара», лимит ~32 — стартовый.
 * Модульный (переживает пересоздание композиции при переключении вкладок), ограничивает
 * число живых битмапов.
 */
private val markerIconCache = LruCache<String, BitmapDescriptor>(MARKER_ICON_CACHE_SIZE)

/**
 * Круглая аватар-иконка маркера друга с неоновым кольцом AURORA: цвет кольца выбирается
 * из [MARKER_NEON_COLORS] детерминированно по [uid] (Math.floorMod от hashCode — всегда
 * неотрицательный индекс). Декод файла (center-crop, круглая маска, кольцо) либо круг
 * с инициалом при отсутствии аватара (паттерн плейсхолдера ProfileAvatar).
 * Декод — в IO-диспетчере, BitmapDescriptorFactory.fromBitmap — только main-поток.
 * Иконка пересоздаётся ТОЛЬКО при смене ключей ([uid], [avatarPath], [displayName]): тики
 * координат двигают маркер через rememberUpdatedMarkerState и к иконке отношения не имеют.
 * Цвета — роли MaterialTheme.colorScheme и палитра маркеров из ui.theme, передаются
 * в отрисовку Int-аргументами.
 */
@Composable
internal fun rememberFriendMarkerIcon(
    uid: String,
    avatarPath: String?,
    displayName: String,
): BitmapDescriptor {
    val circleSizePx = with(LocalDensity.current) { CIRCLE_SIZE_DP.dp.roundToPx() }
    val borderWidthPx = with(LocalDensity.current) { BORDER_WIDTH_DP.dp.roundToPx() }
    val circleColorArgb = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val initialColorArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val ringColorArgb = MARKER_NEON_COLORS[Math.floorMod(uid.hashCode(), MARKER_NEON_COLORS.size)].toArgb()
    val cacheKey = "$uid|$avatarPath"

    // Синхронный плейсхолдер с инициалом: маркер никогда не остаётся без иконки,
    // пока аватар декодируется; ошибка декода оставляет его навсегда.
    val placeholder = remember(
        displayName,
        circleSizePx,
        borderWidthPx,
        circleColorArgb,
        initialColorArgb,
        ringColorArgb,
    ) {
        BitmapDescriptorFactory.fromBitmap(
            renderInitialBitmap(
                displayName = displayName,
                circleSizePx = circleSizePx,
                borderWidthPx = borderWidthPx,
                circleColor = circleColorArgb,
                textColor = initialColorArgb,
                ringColor = ringColorArgb,
            ),
        )
    }

    val iconState = produceState(
        initialValue = markerIconCache.get(cacheKey) ?: placeholder,
        cacheKey,
        displayName,
    ) {
        markerIconCache.get(cacheKey)?.let { cached ->
            value = cached
            return@produceState
        }
        val path = avatarPath
        if (path == null) {
            value = placeholder
            return@produceState
        }
        // Декод файла — IO; BitmapDescriptorFactory.fromBitmap требует main-поток.
        val bitmap = withContext(Dispatchers.IO) {
            decodeCircularAvatar(path, circleSizePx, borderWidthPx, ringColorArgb)
        }
        if (bitmap == null) {
            value = placeholder
            return@produceState
        }
        val icon = BitmapDescriptorFactory.fromBitmap(bitmap)
        markerIconCache.put(cacheKey, icon)
        value = icon
    }
    return iconState.value
}

/** Круг с инициалом: неоновое кольцо, заливка surfaceVariant, первая буква имени. */
private fun renderInitialBitmap(
    displayName: String,
    circleSizePx: Int,
    borderWidthPx: Int,
    circleColor: Int,
    textColor: Int,
    ringColor: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(circleSizePx, circleSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = circleSizePx / 2f
    val radius = center - borderWidthPx
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ringColor }
    canvas.drawCircle(center, center, center, ringPaint)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = circleColor }
    canvas.drawCircle(center, center, radius, fillPaint)

    val initial = displayName.trim().take(1).uppercase().ifEmpty { "?" }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = circleSizePx * INITIAL_TEXT_FRACTION
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    // Вертикальное центрирование по метрикам шрифта, а не по baseline.
    val baseline = center - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(initial, center, baseline, textPaint)
    return bitmap
}

/**
 * Декод файла аватара в круг с неоновым кольцом; null — файл не читается (останется плейсхолдер).
 * Center-crop до квадрата минимальной стороны, масштаб под круг, антиалиасная круглая
 * маска через BitmapShader.
 */
private fun decodeCircularAvatar(
    path: String,
    circleSizePx: Int,
    borderWidthPx: Int,
    ringColor: Int,
): Bitmap? {
    val decoded = BitmapFactory.decodeFile(path) ?: return null
    val side = minOf(decoded.width, decoded.height)
    val square = Bitmap.createBitmap(
        decoded,
        (decoded.width - side) / 2,
        (decoded.height - side) / 2,
        side,
        side,
    )
    val scaled = if (square.width != circleSizePx) {
        Bitmap.createScaledBitmap(square, circleSizePx, circleSizePx, true)
    } else {
        square
    }
    val bitmap = Bitmap.createBitmap(circleSizePx, circleSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = circleSizePx / 2f
    val radius = center - borderWidthPx
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ringColor }
    canvas.drawCircle(center, center, center, ringPaint)
    val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    canvas.drawCircle(center, center, radius, avatarPaint)
    return bitmap
}

/** Сторона круглой иконки маркера; стартовое значение плана. */
private const val CIRCLE_SIZE_DP = 40

/** Толщина кольца иконки; стартовое значение плана. */
private const val BORDER_WIDTH_DP = 3

/** Лимит LruCache иконок: стартовое значение плана. */
private const val MARKER_ICON_CACHE_SIZE = 32

/** Доля стороны круга, занимаемая кеглем инициала. */
private const val INITIAL_TEXT_FRACTION = 0.4f
