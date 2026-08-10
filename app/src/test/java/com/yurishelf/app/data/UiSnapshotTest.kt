package com.yurishelf.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UiSnapshotTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.getSharedPreferences("ui-snapshot-test", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun oldJsonWithoutNewFieldsLoadsWithDefaults() {
        val prefs = context.getSharedPreferences("ui-snapshot-test", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(
                "ui_snapshot_json",
                """{"typeName":"ANIME","query":"百合","sortName":"SCORE","page":3,"viewModeName":"GRID"}""",
            )
            .commit()

        val snapshot = prefs.loadUiSnapshot()

        assertEquals("ANIME", snapshot?.typeName)
        assertEquals("百合", snapshot?.query)
        assertEquals(3, snapshot?.page)
        assertEquals("GRID", snapshot?.viewModeName)
        assertNull(snapshot?.formatName)
        assertNull(snapshot?.aiCategoryName)
        assertNull(snapshot?.winLoseName)
        assertNull(snapshot?.detailKey)
    }

    @Test
    fun saveAndLoadRoundTripsAllFields() = runBlocking {
        val prefs = context.getSharedPreferences("ui-snapshot-test", Context.MODE_PRIVATE)
        val snapshot = UiSnapshot(
            typeName = "GAME",
            query = "百合",
            sortName = "RANK",
            minimumVotes = 10,
            yearOrZero = 2025,
            favoritesOnly = true,
            nsfwOnly = false,
            formatName = "TV",
            aiCategoryName = "LIGHT",
            winLoseName = "WIN",
            page = 7,
            viewModeName = "LIST",
            detailKey = "GAME:123",
        )

        prefs.saveUiSnapshot(snapshot)

        assertEquals(snapshot, prefs.loadUiSnapshot())
    }
}
