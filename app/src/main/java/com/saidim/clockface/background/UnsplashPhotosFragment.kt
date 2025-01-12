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

class UnsplashPhotosFragment : Fragment() {
    private var _binding: FragmentUnsplashGridBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BackgroundSettingsViewModel by activityViewModels()
    private val adapter = SelectedImagesAdapter { position ->
        viewModel.removeImage(position)
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
        observePhotos()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@UnsplashPhotosFragment.adapter
        }
    }

    private fun observePhotos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.collectionPhotos.collect { photos ->
                adapter.submitList(photos)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(collectionId: String): UnsplashPhotosFragment {
            return UnsplashPhotosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_COLLECTION_ID, collectionId)
                }
            }
        }

        private const val ARG_COLLECTION_ID = "collection_id"
    }
} 