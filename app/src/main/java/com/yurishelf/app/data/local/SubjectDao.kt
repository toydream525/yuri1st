package com.yurishelf.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.yurishelf.app.data.mergeCatalogPage
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE catalogType = :catalogType AND isCatalogMember = 1")
    fun observeByCatalogType(catalogType: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isBlocked = 1")
    fun observeBlocked(): Flow<List<SubjectEntity>>

    @Query(
        "SELECT * FROM subjects WHERE id = :subjectId AND catalogType = :catalogType " +
            "LIMIT 1",
    )
    fun observeByKey(subjectId: Int, catalogType: String): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :subjectId AND catalogType = :catalogType")
    suspend fun getByKey(subjectId: Int, catalogType: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE catalogType = :catalogType AND id IN (:subjectIds)")
    suspend fun getByCatalogTypeAndIds(
        catalogType: String,
        subjectIds: List<Int>,
    ): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE isCatalogMember = 1")
    suspend fun getAllCatalogMembers(): List<SubjectEntity>

    @Upsert
    suspend fun upsertAll(subjects: List<SubjectEntity>)

    @Transaction
    suspend fun upsertCatalogPagePreservingFavorites(subjects: List<SubjectEntity>) {
        val merged = subjects.map { mergeCatalogPage(getByKey(it.id, it.catalogType), it) }
        upsertAll(merged)
    }

    @Transaction
    suspend fun upsertDetailPreservingLocalState(subject: SubjectEntity) {
        val existing = getByKey(subject.id, subject.catalogType)
        val merged = subject.copy(
            isFavorite = existing?.isFavorite ?: subject.isFavorite,
            isBlocked = existing?.isBlocked ?: subject.isBlocked,
            isCatalogMember = existing?.isCatalogMember ?: false,
            catalogGeneration = existing?.catalogGeneration ?: 0,
        )
        upsertAll(listOf(merged))
    }

    @Query(
        "UPDATE subjects SET isCatalogMember = 0 " +
            "WHERE catalogType = :catalogType AND nsfw = :nsfw " +
            "AND catalogGeneration != :generation",
    )
    suspend fun deactivateMissingFromPartition(
        catalogType: String,
        nsfw: Boolean,
        generation: Long,
    )

    @Query(
        "UPDATE subjects SET isCatalogMember = 1, nsfw = :nsfw " +
            "WHERE catalogType = :catalogType AND id IN (:subjectIds)",
    )
    suspend fun activateCatalogMembers(
        catalogType: String,
        subjectIds: List<Int>,
        nsfw: Boolean,
    )

    @Query(
        "UPDATE subjects SET isFavorite = :favorite " +
            "WHERE id = :subjectId AND catalogType = :catalogType",
    )
    suspend fun setFavorite(subjectId: Int, catalogType: String, favorite: Boolean)

    @Query(
        "UPDATE subjects SET isBlocked = :blocked " +
            "WHERE id = :subjectId AND catalogType = :catalogType",
    )
    suspend fun setBlocked(subjectId: Int, catalogType: String, blocked: Boolean)

    @Query("DELETE FROM subjects WHERE id = :subjectId AND catalogType = :catalogType")
    suspend fun deleteByKey(subjectId: Int, catalogType: String)

    @Query("SELECT * FROM ai_analyses")
    fun observeAllAnalyses(): Flow<List<AiAnalysisEntity>>

    @Upsert
    suspend fun upsertAiAnalyses(analyses: List<AiAnalysisEntity>)

    @Query(
        "UPDATE subjects SET bangumiCollectionType = :collectionType, " +
            "bangumiCollectionSyncedAt = :syncedAt " +
            "WHERE id = :subjectId AND catalogType = :catalogType",
    )
    suspend fun setBangumiCollection(
        subjectId: Int,
        catalogType: String,
        collectionType: Int?,
        syncedAt: Long,
    )

    @Query(
        "UPDATE subjects SET winLose = :winLose " +
            "WHERE id = :subjectId AND catalogType = :catalogType",
    )
    suspend fun setWinLose(
        subjectId: Int,
        catalogType: String,
        winLose: String?,
    )

    @Query(
        "DELETE FROM ai_analyses WHERE subjectId = :subjectId AND catalogType = :catalogType",
    )
    suspend fun deleteAiAnalysis(subjectId: Int, catalogType: String)
}
