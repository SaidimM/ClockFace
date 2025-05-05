package com.saidim.clockface.background.unsplash

data class UnsplashSearchResultDto(
    val total: Int,
    val totalPages: Int,
    val results: List<UnsplashPhotoDto>
)

data class UnsplashPhotoDto(
    val id: String,
    val urls: PhotoUrlsDto,
    val user: UserDto,
    val blurHash: String
)

data class PhotoUrlsDto(
    val raw: String,
    val full: String,
    val regular: String,
    val small: String,
    val thumb: String
)

data class UserDto(
    val id: String,
    val username: String,
    val name: String,
    val portfolioUrl: String?,
    val bio: String?,
    val location: String?
)

data class UnsplashCollectionDto(
    val id: String,
    val title: String,
    val coverPhoto: UnsplashPhotoDto
)

data class UnsplashTopicDto(
    val id: String,
    val slug: String,
    val title: String
)
