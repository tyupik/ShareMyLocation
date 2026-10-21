package ru.tyupik.sharemylocation.data.profile

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кэш аватаров в каталоге приложения: cache/avatars/{uid}_{version}.jpg.
 * Имя файла включает версию из users/{uid} (avatarVersion), поэтому после загрузки нового
 * аватара путь кэш-файла меняется и UI получает свежее изображение без ручной инвалидации.
 */
@Singleton
class AvatarCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val avatarsDir: File
        get() = File(context.cacheDir, AVATARS_DIR_NAME)

    /** Путь к закэшированному файлу аватара; null — файла с такой версией нет. */
    fun getCachedPath(uid: String, version: Long): String? {
        val file = avatarFile(uid, version)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Записывает байты аватара в кэш и возвращает путь к файлу;
     * остальные кэш-файлы аватара этого пользователя удаляются.
     */
    fun cacheBytes(uid: String, version: Long, bytes: ByteArray): String {
        avatarsDir.mkdirs()
        val file = avatarFile(uid, version)
        file.writeBytes(bytes)
        avatarsDir.listFiles()
            ?.filter { it.name != file.name && it.name.startsWith("${uid}_") }
            ?.forEach { it.delete() }
        return file.absolutePath
    }

    private fun avatarFile(uid: String, version: Long): File =
        File(avatarsDir, "${uid}_${version}.jpg")

    private companion object {
        /** Подкаталог кэша приложения с аватарами. */
        const val AVATARS_DIR_NAME = "avatars"
    }
}
