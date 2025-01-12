package com.saidim.clockface.background.video

import com.google.gson.annotations.SerializedName

data class PexelsVideo(
    val id: Int,
    val width: Int,
    val height: Int,
    val duration: Int,
    @SerializedName("video_files")
    val videoFiles: List<VideoFile>,
    @SerializedName("image")
    val thumbnail: String,
    val user: User
) {
    data class VideoFile(
        val id: Int,
        val quality: String,
        @SerializedName("file_type")
        val fileType: String,
        val width: Int,
        val height: Int,
        val link: String
    )

    data class User(
        val id: Int,
        val name: String,
        val url: String
    )

    fun getBestVideoFile(): VideoFile? {
        return videoFiles.firstOrNull { it.quality == "hd" && it.fileType == "video/mp4" }
            ?: videoFiles.firstOrNull { it.quality == "sd" && it.fileType == "video/mp4" }
            ?: videoFiles.firstOrNull { it.fileType == "video/mp4" }
    }
} 