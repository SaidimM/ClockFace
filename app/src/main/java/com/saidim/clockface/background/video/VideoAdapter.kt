package com.saidim.clockface.background.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.saidim.clockface.R

class VideoAdapter(
    private val onVideoSelected: (PexelsVideo) -> Unit
) : ListAdapter<PexelsVideo, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImage: ImageView = itemView.findViewById(R.id.thumbnailImage)
        private val durationText: TextView = itemView.findViewById(R.id.durationText)
        private val card: MaterialCardView = itemView.findViewById(R.id.card)

        fun bind(video: PexelsVideo) {
            thumbnailImage.load(video.thumbnail) {
                crossfade(true)
                placeholder(R.drawable.placeholder_image)
            }

            durationText.text = formatDuration(video.duration)
            card.setOnClickListener { onVideoSelected(video) }
        }

        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return "%d:%02d".format(minutes, remainingSeconds)
        }
    }

    private class VideoDiffCallback : DiffUtil.ItemCallback<PexelsVideo>() {
        override fun areItemsTheSame(oldItem: PexelsVideo, newItem: PexelsVideo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PexelsVideo, newItem: PexelsVideo): Boolean {
            return oldItem == newItem
        }
    }
} 