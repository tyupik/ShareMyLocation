package ru.tyupik.sharemylocation.business.profile

/**
 * Нормализация и валидация телефона профиля.
 * Российские номера канонизируются к виду 8XXXXXXXXXX независимо от формата ввода:
 * «+79999999999», «+7 999 999-99-99», «89999999999», «+7 (999) 999-99-99», «8 (999) 999-99-99»
 * и т.д. — все дают «89999999999». Номер без кода страны (10 цифр) дополняется восьмёркой.
 * Иностранные номера (11–15 цифр, не RU-шаблон) сохраняют ведущий «+», если он был в вводе.
 */
object PhoneNormalizer {

    /** Минимальное и максимальное число цифр в валидном телефоне (с кодом страны). */
    const val MIN_DIGITS = 10
    const val MAX_DIGITS = 15

    /** Число цифр российского номера с кодом страны. */
    private const val RU_PHONE_DIGITS = 11

    /** Число цифр российского номера без кода страны. */
    private const val RU_PHONE_NO_CODE_DIGITS = 10

    /** Восьмёрка — канонический префикс российских номеров на бэкенде. */
    private const val RU_TRUNK_PREFIX = "8"

    /**
     * Канонизирует [raw] для отправки на бэкенд: российский номер — строго 8XXXXXXXXXX,
     * иначе только цифры с сохранением ведущего «+» исходной строки.
     */
    fun normalize(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.length == RU_PHONE_DIGITS &&
                (digits.first() == '7' || digits.first() == '8') ->
                RU_TRUNK_PREFIX + digits.substring(1)

            digits.length == RU_PHONE_NO_CODE_DIGITS ->
                RU_TRUNK_PREFIX + digits

            else -> if (raw.trimStart().startsWith("+")) "+$digits" else digits
        }
    }

    /** Количество цифр в строке (ведущий «+» не учитывается). */
    fun digitCount(phone: String): Int = phone.count(Char::isDigit)

    /** Валидный телефон — 10–15 цифр после нормализации. */
    fun isValid(phone: String): Boolean = digitCount(phone) in MIN_DIGITS..MAX_DIGITS
}
