package ru.tyupik.sharemylocation.business.groups

/**
 * Бизнес-модель группы друзей из коллекции groups Firestore.
 * Группа — личная «папка» владельца: [memberUids] ⊆ friendUids владельца,
 * участники групп о членстве не знают (шеринг — отдельная фаза).
 * [createdAt] — millis времени создания; 0 — поле отсутствует/рассинхрон.
 */
data class Group(
    val groupId: String,
    val name: String,
    val memberUids: List<String>,
    val createdAt: Long = 0L,
)
