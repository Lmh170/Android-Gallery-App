package com.example.gallery.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.adapter.GridAlbumAdapter
import com.example.gallery.databinding.FragmentGridAlbumBinding
import com.google.android.material.transition.MaterialFadeThrough

class GridAlbumFrag : Fragment() {
    private lateinit var _binding: FragmentGridAlbumBinding
    val binding get() = _binding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (::_binding.isInitialized) return binding.root

        _binding = FragmentGridAlbumBinding.inflate(inflater, container, false)

        val adapter = GridAlbumAdapter(requireParentFragment() as BottomNavFrag)
        binding.rvAlbum.apply {
            this.adapter = adapter
            setHasFixedSize(true)
        }
        adapter.submitList(viewModel.albums.value)
        viewModel.albums.observe(viewLifecycleOwner) { items ->
            val position = (binding.rvAlbum.layoutManager as GridLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
            adapter.submitList(items) {
                if (position == 0) binding.rvAlbum.scrollToPosition(0)
            }
        }
        ViewGroupCompat.setTransitionGroup(binding.rvAlbum, true)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        requireParentFragment().startPostponedEnterTransition()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val position = (binding.rvAlbum.layoutManager as GridLayoutManager)
            .findFirstCompletelyVisibleItemPosition()
        (binding.rvAlbum.adapter as GridAlbumAdapter).submitList(viewModel.albums.value) {
            if (position == 0) binding.rvAlbum.scrollToPosition(0)
        }
    }
}