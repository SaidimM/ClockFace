package com.saidim.clockface.background

import retrofit2.http.GET
import retrofit2.http.Headers
import android.os.Parcel
import android.os.Parcelable

interface UnsplashService {
    companion object {
        // Replace with your Unsplash API access key
        private const val CLIENT_ID = "0E_aPev9Qt0iPOugsqoNrwrtJpbeIJ6a26KZFwok0EM"
        const val BASE_URL = "https://api.unsplash.com/"
    }

    @Headers("Authorization: Client-ID $CLIENT_ID")
    @GET("photos/random?orientation=landscape&query=nature")
    suspend fun getRandomPhoto(): UnsplashPhoto
}

data class UnsplashPhoto(
    val urls: PhotoUrls,
    val user: User
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(PhotoUrls::class.java.classLoader)!!,
        parcel.readParcelable(User::class.java.classLoader)!!
    )

    data class PhotoUrls(
        val regular: String
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString()!!
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(regular)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<PhotoUrls> {
            override fun createFromParcel(parcel: Parcel) = PhotoUrls(parcel)
            override fun newArray(size: Int) = arrayOfNulls<PhotoUrls>(size)
        }
    }
    
    data class User(
        val name: String
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString()!!
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<User> {
            override fun createFromParcel(parcel: Parcel) = User(parcel)
            override fun newArray(size: Int) = arrayOfNulls<User>(size)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(urls, flags)
        parcel.writeParcelable(user, flags)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<UnsplashPhoto> {
        override fun createFromParcel(parcel: Parcel) = UnsplashPhoto(parcel)
        override fun newArray(size: Int) = arrayOfNulls<UnsplashPhoto>(size)
    }
}