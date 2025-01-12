package com.saidim.clockface.background

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
import com.saidim.clockface.background.unsplash.UnsplashCollection

class UnsplashCollectionsAdapter(
    private val onCollectionSelected: (UnsplashCollection) -> Unit
) : ListAdapter<UnsplashCollection, UnsplashCollectionsAdapter.ViewHolder>(CollectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_unsplash_collection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.card)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)

        fun bind(collection: UnsplashCollection) {
            titleText.text = collection.title
            imageView.load(collection.coverPhoto.urls.regular) {
                crossfade(true)
            }
            card.setOnClickListener { onCollectionSelected(collection) }
        }
    }

    private class CollectionDiffCallback : DiffUtil.ItemCallback<UnsplashCollection>() {
        override fun areItemsTheSame(oldItem: UnsplashCollection, newItem: UnsplashCollection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UnsplashCollection, newItem: UnsplashCollection): Boolean {
            return oldItem == newItem
        }
    }
} 