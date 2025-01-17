package com.saidim.clockface.background.unsplash

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.saidim.clockface.background.BackgroundSettingsViewModel
import com.saidim.clockface.background.ImageItem
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
        lifecycleScope.launch { viewModel.searchPhotosByTopic(topic).collect { photoAdapter.submitList(it) } }
    }

    private fun setupRecyclerView() {
        photoAdapter = UnsplashImageAdapter { photo ->
            // Handle photo selection
            viewModel.selectImage(ImageItem.UnsplashImage(photo))
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
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 