package com.saidim.clockface.background

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class UnsplashPagerAdapter(
    activity: FragmentActivity
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UnsplashCollectionsFragment()
            1 -> UnsplashPhotosFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
} 