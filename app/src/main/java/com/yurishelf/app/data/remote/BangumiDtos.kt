package com.yurishelf.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SearchSubjectsRequest(
    val keyword: String,
    val sort: String = "rank",
    val filter: SearchFilter,
)

@Serializable
data class SearchFilter(
    val type: List<Int>,
    val tag: List<String>,
    @SerialName("meta_tags") val metaTags: List<String> = emptyList(),
    @SerialName("air_date") val airDate: List<String> = emptyList(),
    @SerialName("rating_count") val ratingCount: List<String> = emptyList(),
    val nsfw: Boolean,
)

@Serializable
data class PagedSubjectsDto(
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val data: List<SubjectDto> = emptyList(),
)

@Serializable
data class SubjectDto(
    val id: Int,
    val type: Int,
    val name: String,
    @SerialName("name_cn") val nameCn: String? = null,
    val summary: String? = null,
    val date: String? = null,
    val platform: String? = null,
    val images: ImagesDto? = null,
    val rating: RatingDto? = null,
    val collection: CollectionDto? = null,
    @SerialName("meta_tags") val metaTags: List<String> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val infobox: List<InfoboxItemDto> = emptyList(),
    val eps: Int? = null,
    @SerialName("total_episodes") val totalEpisodes: Int? = null,
    val nsfw: Boolean? = null,
)

@Serializable
data class ImagesDto(
    val large: String? = null,
    val common: String? = null,
    val medium: String? = null,
    val small: String? = null,
    val grid: String? = null,
)

@Serializable
data class RatingDto(
    val rank: Int = 0,
    val total: Int = 0,
    val count: Map<String, Int> = emptyMap(),
    val score: Double = 0.0,
)

@Serializable
data class CollectionDto(
    val wish: Int = 0,
    val collect: Int = 0,
    val doing: Int = 0,
    @SerialName("on_hold") val onHold: Int = 0,
    val dropped: Int = 0,
)

@Serializable
data class TagDto(
    val name: String,
    val count: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
data class InfoboxItemDto(
    val key: String,
    val value: JsonElement,
)

@Serializable
data class MeDto(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
)

@Serializable
data class UserCollectionDto(
    @SerialName("subject_id") val subjectId: Int? = null,
    @SerialName("subject_type") val subjectType: Int? = null,
    val type: Int? = null,
    val rate: Int? = null,
    val comment: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("ep_status") val epStatus: Int? = null,
    @SerialName("vol_status") val volStatus: Int? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val private: Boolean? = null,
)

@Serializable
data class UpdateCollectionRequest(
    val type: Int,
    val rate: Int? = null,
    val comment: String? = null,
    val tags: List<String> = emptyList(),
    val private: Boolean = false,
)
