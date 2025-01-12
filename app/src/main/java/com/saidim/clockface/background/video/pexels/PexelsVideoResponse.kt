package com.saidim.clockface.background.video.pexels

data class PexelsVideoResponse(
    val page: Int,
    val per_page: Int,
    val total_results: Int,
    val url: String,
    val videos: List<Video>
)