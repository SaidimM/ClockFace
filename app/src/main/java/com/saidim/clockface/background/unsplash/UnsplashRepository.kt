package com.saidim.clockface.background.unsplash

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class UnsplashRepository {
    private val api: UnsplashApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(UnsplashApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()

        api = retrofit.create(UnsplashApi::class.java)
    }

    suspend fun searchPhotos(
        query: String,
        page: Int = 1,
        perPage: Int = 30,
        orientation: String = "portrait"
    ): UnsplashSearchResultDto {
        return api.searchPhotos(
            query = query,
            page = page,
            perPage = perPage,
            orientation = orientation
        )
    }

    suspend fun getCollectionPhotos(collectionId: String): List<UnsplashPhoto> {
        return try {
            api.getCollectionPhotos(collectionId).map { dto ->
                UnsplashPhoto(
                    id = dto.id,
                    urls = UnsplashPhoto.PhotoUrls(
                        regular = dto.urls.regular,
                        small = dto.urls.small
                    ),
                    user = UnsplashPhoto.User(
                        name = dto.user.name
                    )
                )
            }
        } catch (e: Exception) {
            // Log error and return empty list as fallback
            emptyList()
        }
    }

    suspend fun getCollections(): List<UnsplashCollection> {
        return try {
            api.getCollections().map { dto ->
                UnsplashCollection(
                    id = dto.id,
                    title = dto.title,
                    coverPhoto = UnsplashPhoto(
                        id = dto.coverPhoto.id,
                        urls = UnsplashPhoto.PhotoUrls(
                            regular = dto.coverPhoto.urls.regular,
                            small = dto.coverPhoto.urls.small
                        ),
                        user = UnsplashPhoto.User(
                            name = dto.coverPhoto.user.name
                        )
                    )
                )
            }
        } catch (e: Exception) {
            // Return sample data as fallback
            listOf(
                UnsplashCollection(
                    id = "1",
                    title = "Nature",
                    coverPhoto = UnsplashPhoto(
                        id = "1",
                        urls = UnsplashPhoto.PhotoUrls(
                            regular = "https://images.unsplash.com/photo-1"
                        ),
                        user = UnsplashPhoto.User(
                            name = "John Doe"
                        )
                    )
                )
            )
        }
    }
} 