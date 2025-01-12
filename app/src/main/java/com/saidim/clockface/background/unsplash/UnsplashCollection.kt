package com.saidim.clockface.background.unsplash

data class UnsplashCollection(
    val id: String,
    val title: String,
    val coverPhoto: UnsplashPhoto
)

data class UnsplashPhoto(
    val id: String,
    val urls: PhotoUrls,
    val user: User
) {
    data class PhotoUrls(
        val regular: String,
        val small: String = regular
    )

    data class User(
        val name: String
    )
} 