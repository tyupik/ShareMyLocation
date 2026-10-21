package ru.tyupik.sharemylocation.business.profile

/**
 * Нормализация и валидация телефона профиля.
 * Нормализованный телефон — только цифры с сохранением ведущего «+», если он был в исходной строке.
 */
object PhoneNormalizer {

    /** Минимальное и максимальное число цифр в валидном телефоне (с кодом страны). */
    const val MIN_DIGITS = 10
    const val MAX_DIGITS = 15

    /** Оставляет только цифры и ведущий «+». */
    fun normalize(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return if (raw.trimStart().startsWith("+")) "+$digits" else digits
    }

    /** Количество цифр в строке (ведущий «+» не учитывается). */
    fun digitCount(phone: String): Int = phone.count(Char::isDigit)

    /** Валидный телефон — 10–15 цифр после нормализации. */
    fun isValid(phone: String): Boolean = digitCount(phone) in MIN_DIGITS..MAX_DIGITS
}
