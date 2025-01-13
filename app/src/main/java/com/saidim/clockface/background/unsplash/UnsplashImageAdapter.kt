package com.saidim.clockface.background.unsplash

import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.saidim.clockface.databinding.ItemUnsplashImageBinding

class UnsplashImageAdapter(
    private val onPhotoClick: (UnsplashPhotoDto) -> Unit
) : ListAdapter<UnsplashPhotoDto, UnsplashImageAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(
            ItemUnsplashImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.binding.root.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                onPhotoClick(getItem(position))
            }
        }
    }

    inner class PhotoViewHolder(
        val binding: ItemUnsplashImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: UnsplashPhotoDto) {
            binding.apply {
                // Load the image using Coil
                image.load(photo.urls.small) { crossfade(true) }
                // Set photographer name
                photographerName.text = photo.user.name
            }
        }
    }

    private class PhotoDiffCallback : DiffUtil.ItemCallback<UnsplashPhotoDto>() {
        override fun areItemsTheSame(oldItem: UnsplashPhotoDto, newItem: UnsplashPhotoDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UnsplashPhotoDto, newItem: UnsplashPhotoDto): Boolean {
            return oldItem == newItem
        }
    }
} 