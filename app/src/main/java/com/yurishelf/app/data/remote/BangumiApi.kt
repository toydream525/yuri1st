package com.yurishelf.app.data.remote

import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BangumiApi {
    @POST("v0/search/subjects")
    suspend fun searchSubjects(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Header("Authorization") authorization: String? = null,
        @Body request: SearchSubjectsRequest,
    ): PagedSubjectsDto

    @GET("v0/subjects/{subjectId}")
    suspend fun getSubject(
        @Path("subjectId") subjectId: Int,
        @Header("Authorization") authorization: String? = null,
    ): SubjectDto

    @GET("v0/me")
    suspend fun getMe(
        @Header("Authorization") authorization: String,
    ): MeDto

    @GET("v0/users/{username}/collections/{subjectId}")
    suspend fun getUserCollection(
        @Path("username") username: String,
        @Path("subjectId") subjectId: Int,
        @Header("Authorization") authorization: String,
    ): UserCollectionDto

    @POST("v0/users/-/collections/{subjectId}")
    suspend fun addCollection(
        @Path("subjectId") subjectId: Int,
        @Header("Authorization") authorization: String,
        @Body request: UpdateCollectionRequest,
    ): Unit

    @PATCH("v0/users/-/collections/{subjectId}")
    suspend fun updateCollection(
        @Path("subjectId") subjectId: Int,
        @Header("Authorization") authorization: String,
        @Body request: UpdateCollectionRequest,
    ): Unit
}
