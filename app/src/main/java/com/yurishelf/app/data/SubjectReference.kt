package com.yurishelf.app.data

private val SUBJECT_URL_PATTERN = Regex("""(?:bgm\.tv|bangumi\.tv)/subject/(\d+)""")

/**
 * Extracts a Bangumi subject id from a plain id or a bgm.tv/bangumi.tv
 * subject URL. Returns null when the query is not a subject reference.
 */
fun parseSubjectReference(query: String): Int? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    trimmed.toIntOrNull()?.let { return it }
    return SUBJECT_URL_PATTERN.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()
}
