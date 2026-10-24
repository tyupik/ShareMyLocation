package ru.tyupik.sharemylocation.data.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Источник «снять на камеру» для аватара: создаёт временный JPEG-файл в кэше
 * и его content-Uri через FileProvider для [androidx.activity.result.contract.ActivityResultContracts.TakePicture].
 * Разрешение CAMERA не требуется: приложение выступает лишь получателем снимка камеры.
 */
@Singleton
class CameraAvatarSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Создаёт пустой файл в cache/camera и возвращает его content-Uri через FileProvider. */
    fun createCaptureUri(): String {
        val directory = File(context.cacheDir, CAMERA_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            Log.w(TAG, "Не удалось создать каталог для снимков камеры: $directory")
        }
        val captureFile = File(directory, "avatar_capture_" + System.currentTimeMillis() + ".jpg")
        return FileProvider.getUriForFile(context, authority, captureFile).toString()
    }

    /**
     * Best-effort удаление временного снимка по content-Uri (успех, отмена или сбой съёмки).
     * FileProvider-Uri не содержит реальный путь на диске, поэтому файл восстанавливается
     * обратным маппингом: последний сегмент Uri + каталог cache/camera, куда этот класс
     * кладёт все снимки. Вернётся true только при фактическом удалении файла.
     */
    fun deleteCapturedImage(uri: String): Boolean {
        val fileName = try {
            Uri.parse(uri).lastPathSegment
        } catch (throwable: RuntimeException) {
            Log.w(TAG, "Не удалось разобрать Uri снимка камеры: $uri", throwable)
            null
        }
        if (fileName == null) {
            Log.w(TAG, "Uri снимка камеры без имени файла, удаление пропущено: $uri")
            return false
        }
        val deleted = File(File(context.cacheDir, CAMERA_DIRECTORY), fileName).delete()
        if (!deleted) {
            Log.w(TAG, "Не удалось удалить временный снимок камеры: $fileName")
        }
        return deleted
    }

    /** Authority провайдера совпадает с манифестом (${applicationId}.fileprovider). */
    private val authority: String
        get() = context.packageName + ".fileprovider"

    private companion object {
        const val TAG = "CameraAvatarSource"

        /** Подкаталог кэша для временных снимков; совпадает с path в res/xml/file_paths.xml. */
        const val CAMERA_DIRECTORY = "camera"
    }
}
