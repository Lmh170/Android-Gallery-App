package com.example.gallery

import android.net.Uri
import androidx.core.net.toUri
import androidx.recyclerview.selection.ItemKeyProvider
import com.example.gallery.adapter.GridItemAdapter

class MyItemKeyProvider(private val adapter: GridItemAdapter): ItemKeyProvider<Uri>(SCOPE_CACHED) {
    override fun getKey(position: Int): Uri =
        if (adapter.currentList[position] is ListItem.MediaItem) {
            (adapter.currentList[position] as ListItem.MediaItem).uri
        } else {
            Uri.EMPTY
        }


    override fun getPosition(key: Uri): Int =
        adapter.currentList.indexOfFirst {
            when (it) {
                is ListItem.MediaItem -> {
                    it.uri == key
                }
                is ListItem.Header -> {
                    it.date.toString().toUri() == key
                }
                else -> {
                    false
                }
            }
        }
}