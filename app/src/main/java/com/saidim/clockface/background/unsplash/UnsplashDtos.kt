package com.saidim.clockface.background.unsplash

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UnsplashSearchResultDto(
    @Json(name = "total") val total: Int,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "results") val results: List<UnsplashPhotoDto>
)

@JsonClass(generateAdapter = true)
data class UnsplashPhotoDto(
    @Json(name = "id") val id: String,
    @Json(name = "urls") val urls: PhotoUrlsDto,
    @Json(name = "user") val user: UserDto
)

@JsonClass(generateAdapter = true)
data class PhotoUrlsDto(
    @Json(name = "raw") val raw: String,
    @Json(name = "full") val full: String,
    @Json(name = "regular") val regular: String,
    @Json(name = "small") val small: String,
    @Json(name = "thumb") val thumb: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "name") val name: String,
    @Json(name = "portfolio_url") val portfolioUrl: String?,
    @Json(name = "bio") val bio: String?,
    @Json(name = "location") val location: String?
) 