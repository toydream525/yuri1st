package com.yurishelf.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubjectReferenceTest {
    @Test
    fun parsesPlainId() {
        assertEquals(495291, parseSubjectReference("495291"))
        assertEquals(495291, parseSubjectReference("  495291  "))
    }

    @Test
    fun parsesBgmTvSubjectUrl() {
        assertEquals(495291, parseSubjectReference("https://bgm.tv/subject/495291"))
        assertEquals(495291, parseSubjectReference("https://bangumi.tv/subject/495291"))
        assertEquals(
            495291,
            parseSubjectReference("https://bgm.tv/subject/495291?source=test"),
        )
    }

    @Test
    fun rejectsNonSubjectQueries() {
        assertNull(parseSubjectReference("魔法少女"))
        assertNull(parseSubjectReference("https://bgm.tv/subject/abc"))
        assertNull(parseSubjectReference(""))
        assertNull(parseSubjectReference("   "))
    }
}
