package com.yurishelf.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class YuriShelfDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbNames = mutableListOf<String>()

    @After
    fun tearDown() {
        dbNames.forEach { name ->
            context.deleteDatabase(name)
            val dir = context.getDatabasePath(name).parentFile ?: return@forEach
            File(dir, "$name-wal").delete()
            File(dir, "$name-shm").delete()
        }
    }

    @Test
    fun migrate1To7_preservesRowsAndAddsNewColumns() {
        val name = newDbName()
        rawDb(name).use { db ->
            db.version = 1
            db.execSQL(CREATE_TABLE_V1)
            db.execSQL(INSERT_V1_ROW)
        }

        val database = Room.databaseBuilder(context, YuriShelfDatabase::class.java, name)
            .addMigrations(
                YuriShelfDatabase.MIGRATION_1_2,
                YuriShelfDatabase.MIGRATION_2_3,
                YuriShelfDatabase.MIGRATION_3_4,
                YuriShelfDatabase.MIGRATION_4_5,
                YuriShelfDatabase.MIGRATION_5_6,
                YuriShelfDatabase.MIGRATION_6_7,
            )
            .build()

        try {
            val entity = runBlocking { database.subjectDao().getByKey(42, "anime") }
            assertNotNull(entity)
            assertEquals("示例作品", entity!!.name)
            assertEquals(8.7, entity.ratingScore, 0.0)
            assertEquals(123, entity.ratingTotal)
            assertTrue(entity.isFavorite)
            assertTrue(entity.nsfw)
            assertTrue(entity.isCatalogMember)
            assertEquals(0L, entity.catalogGeneration)
            assertFalse(entity.isBlocked)
            assertEquals(null, entity.bangumiCollectionType)
            assertEquals(0L, entity.bangumiCollectionSyncedAt)
            assertEquals(null, entity.winLose)

            val columns = readColumnNames(database)
            assertTrue(
                columns.containsAll(
                    listOf(
                        "isCatalogMember",
                        "catalogGeneration",
                        "isBlocked",
                        "bangumiCollectionType",
                        "bangumiCollectionSyncedAt",
                        "winLose",
                    ),
                ),
            )
            val aiColumns = readTableNames(database)
            assertTrue("ai_analyses" in aiColumns)
            assertTrue("manualYuriCategory" in columns)
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate3To4_addsBlockedColumnKeepingExistingData() {
        val name = newDbName()
        rawDb(name).use { db ->
            db.version = 3
            db.execSQL(CREATE_TABLE_V1)
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `isCatalogMember` " +
                    "INTEGER NOT NULL DEFAULT 1",
            )
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `catalogGeneration` " +
                    "INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subjects_catalogType` " +
                    "ON `subjects` (`catalogType`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subjects_ratingScore` " +
                    "ON `subjects` (`ratingScore`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subjects_ratingTotal` " +
                    "ON `subjects` (`ratingTotal`)",
            )
            db.execSQL(INSERT_V3_ROW)
        }

        val database = Room.databaseBuilder(context, YuriShelfDatabase::class.java, name)
            .addMigrations(
                YuriShelfDatabase.MIGRATION_3_4,
                YuriShelfDatabase.MIGRATION_4_5,
                YuriShelfDatabase.MIGRATION_5_6,
                YuriShelfDatabase.MIGRATION_6_7,
            )
            .build()

        try {
            val entity = runBlocking { database.subjectDao().getByKey(7, "manga") }
            assertNotNull(entity)
            assertFalse(entity!!.isCatalogMember)
            assertEquals(9L, entity.catalogGeneration)
            assertFalse(entity.isBlocked)
            assertEquals("旧漫画", entity.name)

            val columns = readColumnNames(database)
            assertTrue(columns.contains("bangumiCollectionType"))
            assertTrue(columns.contains("winLose"))
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate4To5_addsCollectionColumnsAndAiTableKeepingExistingData() {
        val name = newDbName()
        rawDb(name).use { db ->
            db.version = 4
            db.execSQL(CREATE_TABLE_V1)
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `isCatalogMember` " +
                    "INTEGER NOT NULL DEFAULT 1",
            )
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `catalogGeneration` " +
                    "INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `isBlocked` " +
                    "INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subjects_catalogType` " +
                    "ON `subjects` (`catalogType`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subjects_ratingScore` " +
                    "ON `subjects` (`ratingScore`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subjects_ratingTotal` " +
                    "ON `subjects` (`ratingTotal`)",
            )
            db.execSQL(INSERT_V4_ROW)
        }

        val database = Room.databaseBuilder(context, YuriShelfDatabase::class.java, name)
            .addMigrations(
                YuriShelfDatabase.MIGRATION_4_5,
                YuriShelfDatabase.MIGRATION_5_6,
                YuriShelfDatabase.MIGRATION_6_7,
            )
            .build()

        try {
            val entity = runBlocking { database.subjectDao().getByKey(9, "game") }
            assertNotNull(entity)
            assertEquals("游戏条目", entity!!.name)
            assertEquals(null, entity.bangumiCollectionType)
            assertEquals(0L, entity.bangumiCollectionSyncedAt)
            assertEquals(null, entity.winLose)

            val columns = readColumnNames(database)
            assertTrue(
                columns.containsAll(listOf("bangumiCollectionType", "bangumiCollectionSyncedAt")),
            )
            val aiColumns = readTableNames(database)
            assertTrue("ai_analyses" in aiColumns)

            runBlocking {
                database.subjectDao().setBangumiCollection(9, "game", 3, 1700000000000)
            }
            val updated = runBlocking { database.subjectDao().getByKey(9, "game") }
            assertEquals(3, updated!!.bangumiCollectionType)
            assertEquals(1700000000000L, updated.bangumiCollectionSyncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate6To7_addsManualCategoryKeepingSubjectsAndAnalyses() {
        val name = newDbName()
        rawDb(name).use { db ->
            db.version = 6
            db.execSQL(CREATE_TABLE_V1)
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `isCatalogMember` " +
                    "INTEGER NOT NULL DEFAULT 1",
            )
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `catalogGeneration` " +
                    "INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `isBlocked` " +
                    "INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `bangumiCollectionType` " +
                    "INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE `subjects` ADD COLUMN `bangumiCollectionSyncedAt` " +
                    "INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL("ALTER TABLE `subjects` ADD COLUMN `winLose` TEXT DEFAULT NULL")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subjects_catalogType` " +
                    "ON `subjects` (`catalogType`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subjects_ratingScore` " +
                    "ON `subjects` (`ratingScore`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subjects_ratingTotal` " +
                    "ON `subjects` (`ratingTotal`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_analyses` (
                    `subjectId` INTEGER NOT NULL,
                    `catalogType` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `confidence` REAL NOT NULL,
                    `reason` TEXT NOT NULL,
                    `riskPointsJson` TEXT NOT NULL,
                    `sourcesJson` TEXT NOT NULL,
                    `analyzedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`subjectId`, `catalogType`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_ai_analyses_catalogType` " +
                    "ON `ai_analyses` (`catalogType`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_ai_analyses_analyzedAt` " +
                    "ON `ai_analyses` (`analyzedAt`)",
            )
            db.execSQL(INSERT_V5_ROW)
            db.execSQL(INSERT_V5_AI_ANALYSIS)
        }

        val database = Room.databaseBuilder(context, YuriShelfDatabase::class.java, name)
            .addMigrations(YuriShelfDatabase.MIGRATION_6_7)
            .build()

        try {
            val entity = runBlocking { database.subjectDao().getByKey(5, "anime") }
            assertNotNull(entity)
            assertEquals("v5作品", entity!!.name)
            assertEquals(null, entity.winLose)
            assertEquals(null, entity.manualYuriCategory)
            val analysis = runBlocking { database.subjectDao().getAiAnalysis(5, "anime") }
            assertNotNull(analysis)
            assertEquals("LIGHT", analysis!!.category)
            assertEquals(0.7, analysis.confidence, 0.0)
            assertEquals("原分析", analysis.reason)
            assertEquals("[]", analysis.riskPointsJson)
            assertEquals("[]", analysis.sourcesJson)
            assertEquals(1700000000000L, analysis.analyzedAt)

            val columns = readColumnNames(database)
            assertTrue(columns.contains("winLose"))
            assertTrue(columns.contains("manualYuriCategory"))

            runBlocking {
                database.subjectDao().setWinLose(5, "anime", "WIN")
            }
            val updated = runBlocking { database.subjectDao().getByKey(5, "anime") }
            assertEquals("WIN", updated!!.winLose)
            runBlocking { database.subjectDao().setManualYuriCategory(5, "anime", "STRONG") }
            assertEquals(
                "STRONG",
                runBlocking { database.subjectDao().getByKey(5, "anime") }!!.manualYuriCategory,
            )
            runBlocking {
                database.subjectDao().upsertDetailPreservingLocalState(
                    updated.copy(name = "详情更新", manualYuriCategory = "LIGHT"),
                )
            }
            assertEquals(
                "STRONG",
                runBlocking { database.subjectDao().getByKey(5, "anime") }!!.manualYuriCategory,
            )
            runBlocking { database.subjectDao().setManualYuriCategory(5, "anime", null) }
            runBlocking {
                database.subjectDao().upsertDetailPreservingLocalState(
                    updated.copy(name = "再次详情更新", manualYuriCategory = "LIGHT"),
                )
            }
            assertEquals(
                null,
                runBlocking { database.subjectDao().getByKey(5, "anime") }!!.manualYuriCategory,
            )
        } finally {
            database.close()
        }
    }

    private fun newDbName(): String =
        "migration-test-${System.nanoTime()}.db".also { dbNames += it }

    private fun rawDb(name: String): SQLiteDatabase {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        return SQLiteDatabase.openOrCreateDatabase(file, null)
    }

    private fun readColumnNames(database: YuriShelfDatabase): Set<String> {
        val names = mutableSetOf<String>()
        database.openHelper.writableDatabase
            .query("PRAGMA table_info(`subjects`)")
            .use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) names += cursor.getString(nameIndex)
            }
        return names
    }

    private fun readTableNames(database: YuriShelfDatabase): Set<String> {
        val names = mutableSetOf<String>()
        database.openHelper.writableDatabase
            .query("SELECT name FROM sqlite_master WHERE type='table'")
            .use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) names += cursor.getString(nameIndex)
            }
        return names
    }

    private companion object {
        val CREATE_TABLE_V1 =
            """
            CREATE TABLE IF NOT EXISTS `subjects` (
                `id` INTEGER NOT NULL,
                `type` INTEGER NOT NULL,
                `catalogType` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `nameCn` TEXT NOT NULL,
                `summary` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `platform` TEXT NOT NULL,
                `imageUrl` TEXT NOT NULL,
                `ratingScore` REAL NOT NULL,
                `ratingTotal` INTEGER NOT NULL,
                `rank` INTEGER NOT NULL,
                `rating1` INTEGER NOT NULL,
                `rating2` INTEGER NOT NULL,
                `rating3` INTEGER NOT NULL,
                `rating4` INTEGER NOT NULL,
                `rating5` INTEGER NOT NULL,
                `rating6` INTEGER NOT NULL,
                `rating7` INTEGER NOT NULL,
                `rating8` INTEGER NOT NULL,
                `rating9` INTEGER NOT NULL,
                `rating10` INTEGER NOT NULL,
                `wish` INTEGER NOT NULL,
                `collect` INTEGER NOT NULL,
                `doing` INTEGER NOT NULL,
                `onHold` INTEGER NOT NULL,
                `dropped` INTEGER NOT NULL,
                `tagsJson` TEXT NOT NULL,
                `metaTagsJson` TEXT NOT NULL,
                `infoboxText` TEXT NOT NULL,
                `episodeCount` INTEGER NOT NULL,
                `isFavorite` INTEGER NOT NULL,
                `nsfw` INTEGER NOT NULL,
                `syncedAt` INTEGER NOT NULL,
                `detailSyncedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`, `catalogType`)
            )
            """.trimIndent()

        val INSERT_V1_ROW =
            """
            INSERT INTO `subjects` (
                `id`, `type`, `catalogType`, `name`, `nameCn`, `summary`, `date`,
                `platform`, `imageUrl`, `ratingScore`, `ratingTotal`, `rank`,
                `rating1`, `rating2`, `rating3`, `rating4`, `rating5`, `rating6`,
                `rating7`, `rating8`, `rating9`, `rating10`, `wish`, `collect`,
                `doing`, `onHold`, `dropped`, `tagsJson`, `metaTagsJson`,
                `infoboxText`, `episodeCount`, `isFavorite`, `nsfw`, `syncedAt`,
                `detailSyncedAt`
            ) VALUES (
                42, 2, 'anime', '示例作品', 'Sample Work', '简介', '2026-01-01',
                'TV', 'https://example.com/cover.jpg', 8.7, 123, 5,
                0, 0, 0, 0, 0, 1,
                2, 3, 4, 5, 10, 20,
                30, 40, 50, '["百合"]', '["百合","动画"]',
                '原作：某人', 12, 1, 1, 1700000000000,
                1700000000000
            )
            """.trimIndent()

        val INSERT_V3_ROW =
            """
            INSERT INTO `subjects` (
                `id`, `type`, `catalogType`, `name`, `nameCn`, `summary`, `date`,
                `platform`, `imageUrl`, `ratingScore`, `ratingTotal`, `rank`,
                `rating1`, `rating2`, `rating3`, `rating4`, `rating5`, `rating6`,
                `rating7`, `rating8`, `rating9`, `rating10`, `wish`, `collect`,
                `doing`, `onHold`, `dropped`, `tagsJson`, `metaTagsJson`,
                `infoboxText`, `episodeCount`, `isFavorite`, `nsfw`, `syncedAt`,
                `detailSyncedAt`, `isCatalogMember`, `catalogGeneration`
            ) VALUES (
                7, 1, 'manga', '旧漫画', 'Old Manga', '旧简介', '2020-01-01',
                '漫画', 'https://example.com/old.jpg', 7.5, 80, 0,
                0, 0, 0, 0, 0, 0,
                0, 1, 2, 3, 4, 5,
                6, 7, 8, '["百合"]', '["漫画"]',
                '作者：某人', 5, 1, 0, 1600000000000,
                1600000000000, 0, 9
            )
            """.trimIndent()

        val INSERT_V4_ROW =
            """
            INSERT INTO `subjects` (
                `id`, `type`, `catalogType`, `name`, `nameCn`, `summary`, `date`,
                `platform`, `imageUrl`, `ratingScore`, `ratingTotal`, `rank`,
                `rating1`, `rating2`, `rating3`, `rating4`, `rating5`, `rating6`,
                `rating7`, `rating8`, `rating9`, `rating10`, `wish`, `collect`,
                `doing`, `onHold`, `dropped`, `tagsJson`, `metaTagsJson`,
                `infoboxText`, `episodeCount`, `isFavorite`, `nsfw`, `syncedAt`,
                `detailSyncedAt`, `isCatalogMember`, `catalogGeneration`, `isBlocked`
            ) VALUES (
                9, 4, 'game', '游戏条目', 'Game Title', '简介', '2024-01-01',
                'PC', 'https://example.com/game.jpg', 8.1, 60, 0,
                0, 0, 0, 0, 0, 0,
                0, 1, 2, 3, 4, 5,
                6, 7, 8, '["百合"]', '["游戏"]',
                '开发商：某人', 1, 0, 0, 1700000000000,
                1700000000000, 1, 0, 0
            )
            """.trimIndent()

        val INSERT_V5_ROW =
            """
            INSERT INTO `subjects` (
                `id`, `type`, `catalogType`, `name`, `nameCn`, `summary`, `date`,
                `platform`, `imageUrl`, `ratingScore`, `ratingTotal`, `rank`,
                `rating1`, `rating2`, `rating3`, `rating4`, `rating5`, `rating6`,
                `rating7`, `rating8`, `rating9`, `rating10`, `wish`, `collect`,
                `doing`, `onHold`, `dropped`, `tagsJson`, `metaTagsJson`,
                `infoboxText`, `episodeCount`, `isFavorite`, `nsfw`, `syncedAt`,
                `detailSyncedAt`, `isCatalogMember`, `catalogGeneration`, `isBlocked`,
                `bangumiCollectionType`, `bangumiCollectionSyncedAt`
            ) VALUES (
                5, 2, 'anime', 'v5作品', 'V5 Work', '简介', '2023-01-01',
                'TV', 'https://example.com/v5.jpg', 8.2, 90, 0,
                0, 0, 0, 0, 0, 0,
                0, 1, 2, 3, 4, 5,
                6, 7, 8, '["百合"]', '["TV"]',
                '原作：某人', 12, 1, 0, 1700000000000,
                1700000000000, 1, 0, 0,
                NULL, 0
            )
            """.trimIndent()

        const val INSERT_V5_AI_ANALYSIS =
            "INSERT INTO `ai_analyses` VALUES (5, 'anime', 'LIGHT', 0.7, '原分析', '[]', '[]', 1700000000000)"
    }
}
