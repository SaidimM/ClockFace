package com.saidim.clockface.clock

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.saidim.clockface.R

class ClockStylesAdapter(
    private val onStyleSelected: (ClockStyle) -> Unit
) : ListAdapter<ClockStyle, ClockStylesAdapter.ViewHolder>(ClockStyleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clock_style, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        private val previewText: TextView = itemView.findViewById(R.id.previewText)
        private val card: MaterialCardView = itemView.findViewById(R.id.card)

        fun bind(style: ClockStyle) {
            titleText.text = style.displayName
            descriptionText.text = style.description
            previewText.text = ClockStyleFormatter.formatTime(style)
            
            when (style) {
                ClockStyle.WORD -> {
                    previewText.textSize = 16f
                    previewText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                else -> {
                    previewText.textSize = 24f
                    previewText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
            }
            
            card.setOnClickListener { 
                val intent = Intent(itemView.context, ClockStyleEditorActivity::class.java).apply {
                    putExtra(ClockStyleEditorActivity.EXTRA_STYLE, style)
                }
                
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    itemView.context as AppCompatActivity,
                    previewText,
                    ClockStyleEditorActivity.SHARED_ELEMENT_NAME
                )
                
                itemView.context.startActivity(intent, options.toBundle())
            }
        }
    }

    private class ClockStyleDiffCallback : DiffUtil.ItemCallback<ClockStyle>() {
        override fun areItemsTheSame(oldItem: ClockStyle, newItem: ClockStyle): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ClockStyle, newItem: ClockStyle): Boolean {
            return oldItem == newItem
        }
    }
} 