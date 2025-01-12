package com.saidim.clockface.background

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.saidim.clockface.R

class SelectedImagesAdapter(
    private val onItemRemove: (Int) -> Unit
) : ListAdapter<ImageItem, SelectedImagesAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val removeButton: ImageView = itemView.findViewById(R.id.removeButton)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)

        init {
            removeButton.setOnClickListener {
                onItemRemove(adapterPosition)
            }
        }

        fun bind(item: ImageItem) {
            imageView.load(item.getThumbnailUrl()) {
                crossfade(true)
                placeholder(R.drawable.placeholder_image)
                error(R.drawable.error_image)
            }
        }
    }

    private class ImageDiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return when {
                oldItem is ImageItem.DeviceImage && newItem is ImageItem.DeviceImage ->
                    oldItem.uri == newItem.uri
                oldItem is ImageItem.UnsplashImage && newItem is ImageItem.UnsplashImage ->
                    oldItem.photo.urls.regular == newItem.photo.urls.regular
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return oldItem == newItem
        }
    }
} 