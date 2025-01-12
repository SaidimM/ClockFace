package com.saidim.clockface.utils

import com.saidim.clockface.background.video.pexels.Video
import com.saidim.clockface.background.video.pexels.VideoFile


fun Video.getBestVideoFile(): VideoFile? {
    return this.video_files.firstOrNull { it.quality == "hd" && it.file_type == "video/mp4" }
        ?: this.video_files.firstOrNull { it.quality == "sd" && it.file_type == "video/mp4" }
        ?: this.video_files.firstOrNull { it.file_type == "video/mp4" }
}