package com.saidim.clockface.background.unsplash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.saidim.clockface.background.BackgroundSettingsViewModel
import com.saidim.clockface.background.model.BackgroundModel.ImageModel
import com.saidim.clockface.databinding.FragmentUnsplashGridBinding
import kotlinx.coroutines.launch

class TopicPhotosFragment(private val topic: String) : Fragment() {
    private var _binding: FragmentUnsplashGridBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BackgroundSettingsViewModel by activityViewModels()
    private lateinit var photoAdapter: UnsplashImageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUnsplashGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeSearchResults()
        
        // Show loading indicator before API call
        binding.imageGridLoadingIndicator?.visibility = View.VISIBLE
        
        lifecycleScope.launch { 
            viewModel.searchPhotosByTopic(topic).collect { results -> 
                photoAdapter.submitList(results) 
                // Hide loading indicator once results are available
                binding.imageGridLoadingIndicator?.visibility = View.GONE
            } 
        }
    }

    private fun setupRecyclerView() {
        photoAdapter = UnsplashImageAdapter { photo ->
            // Handle photo selection
            viewModel.selectImage(ImageModel(photo.urls.raw))
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = photoAdapter
        }
    }

    private fun observeSearchResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResults.collect { photos ->
                photoAdapter.submitList(photos)
                // Hide loading indicator when search results are updated
                binding.imageGridLoadingIndicator?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 