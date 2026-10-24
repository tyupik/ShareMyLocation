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
 * Источник файла-результата кадрирования аватара: формирует путь к JPEG в кэше
 * и его content-Uri через FileProvider как customOutputUri для CropImageActivity
 * (библиотека android-image-cropper по соображениям безопасности принимает
 * на запись только content://, file:// отклоняет с SecurityException).
 */
@Singleton
class AvatarCropOutputSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Формирует путь к файлу-результату в cache/crop и возвращает его content-Uri через FileProvider. */
    fun createOutputUri(): String {
        val directory = File(context.cacheDir, CROP_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            Log.w(TAG, "Не удалось создать каталог для результатов кадрирования: $directory")
        }
        val outputFile = File(directory, "avatar_crop_" + System.currentTimeMillis() + ".jpg")
        return FileProvider.getUriForFile(context, authority, outputFile).toString()
    }

    /**
     * Best-effort удаление файла-результата кадрирования (успех, отмена или сбой кропа).
     * content-Uri FileProvider не содержит реальный путь на диске, поэтому основной путь —
     * delete через ContentResolver (FileProvider поддерживает его для cache-path);
     * fallback — удаление по пути из Uri. Вернётся true только при фактическом удалении.
     */
    fun deleteOutput(uri: String): Boolean {
        val parsedUri = Uri.parse(uri)
        val deleted = try {
            context.contentResolver.delete(parsedUri, null, null) > 0
        } catch (throwable: RuntimeException) {
            Log.w(TAG, "ContentResolver.delete не сработал для результата кадрирования: $uri", throwable)
            File(parsedUri.path ?: "").delete()
        }
        if (!deleted) {
            Log.w(TAG, "Не удалось удалить временный файл кадрирования: $uri")
        }
        return deleted
    }

    /** Authority провайдера совпадает с манифестом (${applicationId}.fileprovider). */
    private val authority: String
        get() = context.packageName + ".fileprovider"

    private companion object {
        const val TAG = "AvatarCropOutputSource"

        /** Подкаталог кэша для результатов кадрирования; совпадает с path в res/xml/file_paths.xml. */
        const val CROP_DIRECTORY = "crop"
    }
}
