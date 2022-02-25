package com.example.gallery

import android.content.ClipData.Item
import android.util.SparseArray
import androidx.collection.LongSparseArray
import androidx.lifecycle.LiveData
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.gallery.ui.*

// an adapted version of StableIdKeyProvider as it cannot handle changing datasets
class MyItemKeyProvider(private val viewModel: MainViewModel, private val isAlbum: Boolean = false): ItemKeyProvider<Long>(SCOPE_CACHED) {
    private val mPositionToKey = SparseArray<Long>()
    private val mKeyToPosition = LongSparseArray<Int>()
/*
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
        frag: BottomNavFrag
    ) : this() {
        items.observe(frag.viewLifecycleOwner) {
            it.forEachIndexed { index, listItem ->
                mPositionToKey.put(index, listItem.id)
                mKeyToPosition.put(listItem.id, index)
            }
        }
    }

 */

    override fun getKey(position: Int): Long? {
        return if (isAlbum) {
            viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems
            ?.get(position)?.id
        } else {
            viewModel.recyclerViewItems.value?.get(position)?.id
        }
    }
        // mPositionToKey.get(position)


    override fun getPosition(key: Long): Int {
        return if (isAlbum) {
            viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems
                ?.indexOfFirst { it.id == key } ?: RecyclerView.NO_POSITION
        } else {
            viewModel.recyclerViewItems.value?.indexOfFirst { it.id == key } ?: RecyclerView.NO_POSITION
        }
    }
       // mKeyToPosition.get(key, RecyclerView.NO_POSITION)
}


/*
class MyItemKeyProvider() : ItemKeyProvider<Long>(SCOPE_CACHED) {
    private lateinit var items: List<ListItem>

    constructor(
        albums: LiveData<List<Album>>,
        frag: AlbumDetailFrag
    ) : this() {
        albums.observe(frag.viewLifecycleOwner) { list ->
            items = list.find { it.name == MainActivity.currentAlbumName }?.mediaItems!!
        }
    }

    constructor(
        items: LiveData<List<ListItem>>,
        frag: GridItemFrag
    ) : this() {
        items.observe(frag.viewLifecycleOwner) {
            this.items = it
        }
    }

    override fun getKey(position: Int): Long {
        return items[position].id
    }

    override fun getPosition(key: Long): Int {
        items.forEachIndexed { index, listItem ->
            if (listItem.id == key) return index
        }
        println("shit shit shit")
        return RecyclerView.NO_POSITION
    }
}

 */

