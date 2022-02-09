package com.example.gallery

import android.util.SparseArray
import androidx.collection.LongSparseArray
import androidx.lifecycle.LiveData
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.gallery.ui.AlbumDetailFrag
import com.example.gallery.ui.GridItemFrag
import com.example.gallery.ui.MainActivity

// an adapted version of StableIdKeyProvider as it cannot handle changing datasets
class MyItemKeyProvider(): ItemKeyProvider<Long>(SCOPE_CACHED) {
    private val mPositionToKey = SparseArray<Long>()
    private val mKeyToPosition = LongSparseArray<Int>()

    constructor(
        albums: LiveData<List<Album>>,
        frag: AlbumDetailFrag
    ) : this() {
        albums.observe(frag.viewLifecycleOwner) { list ->
            list.find { it.name == MainActivity.currentAlbumName }?.mediaItems?.forEachIndexed { index, mediaItem ->
                mPositionToKey.put(index, mediaItem.id)
                mKeyToPosition.put(mediaItem.id, index)
            }
        }
    }

    constructor(
        items: LiveData<List<ListItem>>,
        frag: GridItemFrag
    ) : this() {
        items.observe(frag.viewLifecycleOwner) {
            it.forEachIndexed { index, listItem ->
                mPositionToKey.put(index, listItem.id)
                mKeyToPosition.put(listItem.id, index)
            }
        }
    }

    override fun getKey(position: Int): Long? =
        mPositionToKey.get(position)


    override fun getPosition(key: Long): Int =
        mKeyToPosition.get(key, RecyclerView.NO_POSITION)
}