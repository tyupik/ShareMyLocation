package ru.tyupik.sharemylocation.business.contacts

import javax.inject.Inject
import javax.inject.Singleton

/** Реализация интерактора телефонной книги поверх репозитория. */
@Singleton
class ContactsInteractorImpl @Inject constructor(
    private val contactsRepository: ContactsRepository,
) : ContactsInteractor {

    override suspend fun getPhoneForSearch(contactUri: String): Result<String> {
        val phone = contactsRepository.readFirstPhone(contactUri)
            .getOrElse { throwable -> return Result.failure(throwable) }
            ?: return Result.failure(ContactsException(ContactsError.NoPhone))
        return Result.success(phone)
    }
}
