package com.example.gallery

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

sealed class ListItem {

    abstract val id: Long

    data class MediaItem(
        override val id: Long, val uri: Uri, val album: String, val type: Int,
        val dateModified: Long, val viewPagerPosition: Int, val listPosition: Int
    ) : ListItem() {

        companion object {
            val DiffCallback: DiffUtil.ItemCallback<MediaItem> =
                object : DiffUtil.ItemCallback<MediaItem>() {
                    override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                        oldItem.id == newItem.id

                    override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                        oldItem == newItem
                }
        }
    }

    data class Header(override val id: Long, val description: String? = null) : ListItem()

    class Search(var query: String? = null) : ListItem() {
        override val id: Long = 0x12332
    }

    companion object {
        val DiffCallback: DiffUtil.ItemCallback<ListItem> =
            object : DiffUtil.ItemCallback<ListItem>() {
                override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean =
                    oldItem == newItem
            }
    }
}







