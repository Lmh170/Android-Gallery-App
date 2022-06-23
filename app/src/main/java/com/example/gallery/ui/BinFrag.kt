package com.example.gallery.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.example.gallery.R
import com.example.gallery.adapter.GridItemAdapter

class BinFrag : AlbumFrag() {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.loadBin()

        viewModel.binItems.observe(viewLifecycleOwner) {
            (binding.rvAlbumDetail.adapter as GridItemAdapter).submitList(it) {
                scrollToFirst(binding.rvAlbumDetail)
            }
        }

        setUpAlbumFrag(
            inflater,
            container,
            R.id.action_binFrag_to_viewPagerFrag
        )

        binding.tbAlbum.title = resources.getString(R.string.bin)

        binding.tbAlbum.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }
}