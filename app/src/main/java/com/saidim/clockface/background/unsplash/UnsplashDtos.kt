package com.saidim.clockface.background.unsplash

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UnsplashCollectionDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "cover_photo") val coverPhoto: UnsplashPhotoDto
)

@JsonClass(generateAdapter = true)
data class UnsplashPhotoDto(
    @Json(name = "id") val id: String,
    @Json(name = "urls") val urls: PhotoUrlsDto,
    @Json(name = "user") val user: UserDto
) {
    @JsonClass(generateAdapter = true)
    data class PhotoUrlsDto(
        @Json(name = "regular") val regular: String,
        @Json(name = "small") val small: String
    )

    @JsonClass(generateAdapter = true)
    data class UserDto(
        @Json(name = "name") val name: String
    )
} 