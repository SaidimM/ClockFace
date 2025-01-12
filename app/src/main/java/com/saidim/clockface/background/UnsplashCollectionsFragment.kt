package com.saidim.clockface.background

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.saidim.clockface.databinding.FragmentUnsplashGridBinding
import kotlinx.coroutines.launch

class UnsplashCollectionsFragment : Fragment() {
    private var _binding: FragmentUnsplashGridBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BackgroundSettingsViewModel by activityViewModels()
    private val adapter = UnsplashCollectionsAdapter { collection ->
        viewModel.selectCollection(collection)
    }

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
        observeCollections()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@UnsplashCollectionsFragment.adapter
        }
    }

    private fun observeCollections() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.collections.collect { collections ->
                adapter.submitList(collections)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 