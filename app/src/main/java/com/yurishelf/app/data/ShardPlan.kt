package com.yurishelf.app.data

data class YearRange(
    val startYear: Int,
    val endYearExclusive: Int,
)

/**
 * Splits [startYear]..[endYearExclusive) into chunks of at most [span] years.
 * Used to shard catalog queries by air date so each request stays well below
 * the API's 1000-result pagination cap.
 */
fun yearChunks(
    startYear: Int,
    endYearExclusive: Int,
    span: Int = 5,
): List<YearRange> {
    require(startYear < endYearExclusive) { "startYear must be before endYearExclusive" }
    require(span > 0) { "span must be positive" }
    val chunks = mutableListOf<YearRange>()
    var current = startYear
    while (current < endYearExclusive) {
        val next = minOf(current + span, endYearExclusive)
        chunks += YearRange(current, next)
        current = next
    }
    return chunks
}
