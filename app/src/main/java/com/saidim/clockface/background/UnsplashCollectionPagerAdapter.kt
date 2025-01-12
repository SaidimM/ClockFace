package com.saidim.clockface.background

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.saidim.clockface.background.unsplash.UnsplashCollection

class UnsplashCollectionPagerAdapter(
    private val collections: List<UnsplashCollection>,
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = collections.size

    override fun createFragment(position: Int): Fragment {
        return UnsplashPhotosFragment.newInstance(collections[position].id)
    }
} 