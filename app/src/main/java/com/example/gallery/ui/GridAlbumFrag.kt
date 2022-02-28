package com.example.gallery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.R
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
        viewModel.albums.observe(viewLifecycleOwner) { items ->
            val position = (binding.rvAlbum.layoutManager as GridLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
            (binding.rvAlbum.adapter as GridAlbumAdapter).submitList(items) {
                if (position == 0) binding.rvAlbum.scrollToPosition(0)
            }
        }
        if (::_binding.isInitialized){
            binding.rvAlbum.layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.spanCount).div(2))
            return binding.root
        }

        _binding = FragmentGridAlbumBinding.inflate(inflater, container, false)

        val adapter = GridAlbumAdapter(requireParentFragment() as BottomNavFrag)
        binding.rvAlbum.apply {
            this.adapter = adapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.spanCount).div(2))
        }

        ViewGroupCompat.setTransitionGroup(binding.rvAlbum, true)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        requireParentFragment().startPostponedEnterTransition()
        return binding.root
    }

}