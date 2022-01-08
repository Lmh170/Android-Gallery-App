package com.example.gallery

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

sealed class ListItem {

    abstract val id: Long

    data class MediaItem (val name: String, val size: Long, override val id: Long, val uri: Uri,
                          val dateAdded: Long, val dateTaken: Long, val dateModified: Long, val album: String, val duration: Int, val type: Int,
                          val listPosition: Int, val viewPagerPosition: Int, val path: String): ListItem() {

        companion object {
            val DiffCallback = object : DiffUtil.ItemCallback<MediaItem>() {
                override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                    oldItem == newItem
            }
        }

    }

    data class Header(val date: Long): ListItem() {
        override val id = Long.MIN_VALUE
    }

    class ListItemDiffCallback: DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return if (oldItem is ListItem.MediaItem && newItem is ListItem.MediaItem) {
                oldItem.uri == newItem.uri
            } else {
                oldItem == newItem
            }
        }

    }
}








