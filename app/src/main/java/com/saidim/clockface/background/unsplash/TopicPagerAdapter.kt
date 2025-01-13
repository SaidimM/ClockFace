package com.saidim.clockface.background.unsplash

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TopicPagerAdapter(
    private val topics: List<String>,
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = topics.size

    override fun createFragment(position: Int): Fragment {
        return TopicPhotosFragment.newInstance(topics[position])
    }
} 