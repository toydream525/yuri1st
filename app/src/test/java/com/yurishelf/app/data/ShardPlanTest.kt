package com.yurishelf.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ShardPlanTest {
    @Test
    fun splitsRangeIntoFiveYearChunks() {
        assertEquals(
            listOf(YearRange(1980, 1985), YearRange(1985, 1990), YearRange(1990, 1992)),
            yearChunks(1980, 1992, span = 5),
        )
    }

    @Test
    fun singleYearRangeWhenRangeFitsSpan() {
        assertEquals(listOf(YearRange(2020, 2021)), yearChunks(2020, 2021, span = 5))
    }
}
