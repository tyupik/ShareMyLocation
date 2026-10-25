package ru.tyupik.sharemylocation.data.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.tyupik.sharemylocation.business.contacts.ContactsError
import ru.tyupik.sharemylocation.business.contacts.ContactsException
import ru.tyupik.sharemylocation.business.contacts.ContactsRepository
import ru.tyupik.sharemylocation.data.common.mapFailure
import ru.tyupik.sharemylocation.data.common.runSuspendCatching

/**
 * Реализация чтения телефонной книги поверх ContactsProvider.
 * READ_CONTACTS не запрашивается: Uri приходит от системного пикера
 * (ACTION_PICK на таблицу телефонов [ContactsContract.CommonDataKinds.Phone]) и уже снабжён
 * временным read-грантом на этот конкретный номер, поэтому читаем его напрямую по Uri.
 * Все исключения чтения заворачиваются в [ContactsError.ReadFailed].
 */
@Singleton
class ContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ContactsRepository {

    override suspend fun readFirstPhone(contactUri: String): Result<String?> = runSuspendCatching {
        withContext(Dispatchers.IO) { resolveFirstPhone(contactUri) }
    }.mapFailure { throwable ->
        ContactsException(ContactsError.ReadFailed(throwable.message), throwable)
    }

    /**
     * Читает NUMBER прямо по Uri телефона от пикера; null-курсор или пустой результат —
     * null (интерактор завернёт в NoPhone).
     */
    private fun resolveFirstPhone(contactUri: String): String? {
        val resolver = context.contentResolver
        return resolver.query(
            Uri.parse(contactUri),
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val phone = cursor.getString(0)
                if (!phone.isNullOrBlank()) return phone
            }
            null
        }
    }
}
