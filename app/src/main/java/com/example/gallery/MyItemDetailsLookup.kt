package com.example.gallery

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.example.gallery.adapter.GridItemAdapter

class MyItemDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<Long>() {

    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y) ?: return null
        val viewHolder = recyclerView.getChildViewHolder(view) ?: return null

        return if (viewHolder is GridItemAdapter.MediaItemHolder) {
            viewHolder.getItemDetails()
        } else null
    }
}