package ru.tyupik.sharemylocation.data.contacts

import android.content.ContentUris
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
 * READ_CONTACTS не запрашивается: Uri приходит от системного контакт-пикера, который выдаёт
 * временный read-грант на выбранный контакт; грант действует только на этот Uri и отзывчив
 * после перезапуска процесса, поэтому телефон читается сразу по возврату с пикера.
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
     * Канонизация Uri через [ContactsContract.Contacts.lookupContact] (пикер отдаёт
     * lookup-Uri), затем запрос таблицы телефонов по CONTACT_ID с приоритетом
     * IS_SUPER_PRIMARY → IS_PRIMARY; берётся первый непустой номер.
     */
    private fun resolveFirstPhone(contactUri: String): String? {
        val resolver = context.contentResolver
        val lookupUri = ContactsContract.Contacts.lookupContact(resolver, Uri.parse(contactUri))
            ?: return null
        val contactId = ContentUris.parseId(lookupUri)
        return resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            "${ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY} DESC, " +
                "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val phone = cursor.getString(0)
                if (!phone.isNullOrBlank()) return phone
            }
            null
        }
    }
}
