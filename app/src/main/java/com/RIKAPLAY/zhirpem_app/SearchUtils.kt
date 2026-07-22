package com.RIKAPLAY.zhirpem_app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Универсальная функция для поиска по списку (посты, пользователи и т.д.).
 * 
 * @param query Поисковый запрос.
 * @param items Исходный список элементов.
 * @param selector Функция для выбора поля, по которому будет идти поиск.
 * @return Список отфильтрованных элементов.
 */
suspend fun <T> searchItems(
    query: String,
    items: List<T>,
    selector: (T) -> String
): List<T> {
    if (query.isBlank()) return items

    // Нормализация запроса
    val normalizedQuery = query.lowercase()

    val searchAction = {
        // Подготовка Regex для "умного" поиска
        val regex = try {
            Regex(query, RegexOption.IGNORE_CASE)
        } catch (e: Exception) {
            null
        }

        items.filter { item ->
            val fieldValue = selector(item).lowercase() // Нормализация поля

            // Базовый поиск через contains
            val containsMatch = fieldValue.contains(normalizedQuery, ignoreCase = true)

            // Умный поиск через Regex (если запрос — валидное регулярное выражение)
            val regexMatch = regex?.containsMatchIn(fieldValue) ?: false

            containsMatch || regexMatch
        }
    }

    // Если элементов много (> 100), выполняем в фоновом потоке Dispatchers.Default
    return if (items.size > 100) {
        withContext(Dispatchers.Default) {
            searchAction()
        }
    } else {
        searchAction()
    }
}

/**
 * Специализированный поиск по постам.
 */
suspend fun searchPosts(query: String, posts: List<Post>): List<Post> {
    return searchItems(query, posts) { it.text }
}

/**
 * Специализированный поиск по пользователям.
 */
suspend fun searchUsers(query: String, users: List<User>): List<User> {
    return searchItems(query, users) { "${it.name} ${it.username} ${it.id}" }
}
