package com.example.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil

data class Album (var name: String, var _mediaItems: MutableLiveData<List<ListItem.MediaItem>>){
    val mediaItems: LiveData<List<ListItem.MediaItem>> get() = _mediaItems

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Album>() {
            override fun areItemsTheSame(oldItem: Album, newItem: Album) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Album, newItem: Album) =
                oldItem.mediaItems.value == newItem.mediaItems.value
        }
    }
}