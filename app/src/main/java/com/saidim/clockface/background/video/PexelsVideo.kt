package com.saidim.clockface.background.video

import com.google.gson.annotations.SerializedName

data class PexelsVideo(
    val id: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Int = 0,
    @SerializedName("video_files")
    val videoFiles: List<VideoFile> = listOf<VideoFile>(),
    @SerializedName("image")
    val thumbnail: String = "",
    val user: User = User()
) {
    data class VideoFile(
        val id: Int = 0,
        val quality: String = "",
        @SerializedName("file_type")
        val fileType: String = "",
        val width: Int = 0,
        val height: Int = 0,
        val link: String = ""
    )

    data class User(
        val id: Int = 0,
        val name: String = "",
        val url: String = ""
    )

    fun getBestVideoFile(): VideoFile? {
        return videoFiles.firstOrNull { it.quality == "hd" && it.fileType == "video/mp4" }
            ?: videoFiles.firstOrNull { it.quality == "sd" && it.fileType == "video/mp4" }
            ?: videoFiles.firstOrNull { it.fileType == "video/mp4" }
    }
} 