package com.saidim.clockface.background.video.pexels

import com.saidim.clockface.background.video.PexelsVideo

data class PexelsVideoResponse(
    val page: Int,
    val per_page: Int,
    val total_results: Int,
    val url: String,
    val videos: List<PexelsVideo>
)