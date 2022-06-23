package com.example.gallery

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.gallery.ui.MainActivity
import com.example.gallery.ui.MainViewModel
import com.example.gallery.ui.MediaFrag

class MyItemKeyProvider(
    private val viewModel: MainViewModel,
    private val albumName: String?
) : ItemKeyProvider<Long>(SCOPE_CACHED) {

    override fun getKey(position: Int): Long? {
        return when {
            albumName == MediaFrag.binFragID -> {
                viewModel.binItems.value?.get(position)?.id
            }
            albumName != null -> {
                viewModel.albums.value?.find { it.name == albumName }?.mediaItems
                    ?.get(position)?.id
            }
            else -> {
                viewModel.recyclerViewItems.value?.get(position)?.id
            }
        }
    }

    override fun getPosition(key: Long): Int {
        return when {
            albumName == MediaFrag.binFragID -> {
                viewModel.binItems.value?.indexOfFirst { it.id == key }
                    ?: RecyclerView.NO_POSITION
            }
            albumName != null -> {
                viewModel.albums.value?.find { it.name == albumName }?.mediaItems
                    ?.indexOfFirst { it.id == key } ?: RecyclerView.NO_POSITION
            }
            else -> {
                viewModel.recyclerViewItems.value?.indexOfFirst { it.id == key }
                    ?: RecyclerView.NO_POSITION
            }
        }
    }
}

