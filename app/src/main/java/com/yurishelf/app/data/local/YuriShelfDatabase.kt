package com.yurishelf.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SubjectEntity::class, AiAnalysisEntity::class],
    version = 7,
    exportSchema = true,
)
abstract class YuriShelfDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subjects_new` (
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
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO `subjects_new` (
                        `id`, `type`, `catalogType`, `name`, `nameCn`, `summary`, `date`,
                        `platform`, `imageUrl`, `ratingScore`, `ratingTotal`, `rank`,
                        `rating1`, `rating2`, `rating3`, `rating4`, `rating5`, `rating6`,
                        `rating7`, `rating8`, `rating9`, `rating10`, `wish`, `collect`,
                        `doing`, `onHold`, `dropped`, `tagsJson`, `metaTagsJson`,
                        `infoboxText`, `episodeCount`, `isFavorite`, `nsfw`, `syncedAt`,
                        `detailSyncedAt`
                    )
                    SELECT
                        `id`, `type`, `catalogType`, `name`, `nameCn`, `summary`, `date`,
                        `platform`, `imageUrl`, `ratingScore`, `ratingTotal`, `rank`,
                        `rating1`, `rating2`, `rating3`, `rating4`, `rating5`, `rating6`,
                        `rating7`, `rating8`, `rating9`, `rating10`, `wish`, `collect`,
                        `doing`, `onHold`, `dropped`, `tagsJson`, `metaTagsJson`,
                        `infoboxText`, `episodeCount`, `isFavorite`, `nsfw`, `syncedAt`,
                        `detailSyncedAt`
                    FROM `subjects`
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE `subjects`")
                database.execSQL("ALTER TABLE `subjects_new` RENAME TO `subjects`")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_subjects_catalogType` " +
                        "ON `subjects` (`catalogType`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_subjects_ratingScore` " +
                        "ON `subjects` (`ratingScore`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_subjects_ratingTotal` " +
                        "ON `subjects` (`ratingTotal`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `subjects` ADD COLUMN `isCatalogMember` " +
                        "INTEGER NOT NULL DEFAULT 1",
                )
                database.execSQL(
                    "ALTER TABLE `subjects` ADD COLUMN `catalogGeneration` " +
                        "INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `subjects` ADD COLUMN `isBlocked` " +
                        "INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `subjects` ADD COLUMN `bangumiCollectionType` " +
                        "INTEGER DEFAULT NULL",
                )
                database.execSQL(
                    "ALTER TABLE `subjects` ADD COLUMN `bangumiCollectionSyncedAt` " +
                        "INTEGER NOT NULL DEFAULT 0",
                )
                database.execSQL(
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
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ai_analyses_catalogType` " +
                        "ON `ai_analyses` (`catalogType`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ai_analyses_analyzedAt` " +
                        "ON `ai_analyses` (`analyzedAt`)",
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `subjects` ADD COLUMN `winLose` TEXT DEFAULT NULL",
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `subjects` ADD COLUMN `manualYuriCategory` TEXT DEFAULT NULL",
                )
            }
        }
    }
}
