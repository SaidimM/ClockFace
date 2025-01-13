package com.saidim.clockface.background.unsplash

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.fragment.app.FragmentManager

class TopicPagerAdapter(
    private val topics: List<String>,
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = mutableMapOf<Int, Fragment>()

    override fun getItemCount(): Int = topics.size

    override fun createFragment(position: Int): Fragment {
        return TopicPhotosFragment(topics[position])
    }

    // Each position should have a unique ID
    override fun getItemId(position: Int): Long {
        return topics[position].hashCode().toLong()
    }

    // Check if the item ID exists in our dataset
    override fun containsItem(itemId: Long): Boolean {
        return topics.any { it.hashCode().toLong() == itemId }
    }

    fun clearFragments() {
        fragments.clear()
    }
} 