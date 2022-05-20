package com.example.gallery.adapter

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.example.gallery.GlideApp
import com.example.gallery.ListItem
import com.example.gallery.databinding.ListGridHeaderBinding
import com.example.gallery.databinding.ListGridMediaItemHolderBinding
import com.example.gallery.ui.BinFrag
import com.example.gallery.ui.BottomNavFrag
import com.example.gallery.ui.MainActivity
import com.google.android.material.shape.ShapeAppearanceModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class FlexItemAdapter(val frag: BottomNavFrag) :
    ListAdapter<ListItem, RecyclerView.ViewHolder>(ListItem.ListItemDiffCallback) {

    private val enterTransitionStarted: AtomicBoolean = AtomicBoolean()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            GridItemAdapter.ITEM_VIEW_TYPE_HEADER -> HeaderViewHolder(
                ListGridHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
            else -> MediaItemHolder(
                ListGridMediaItemHolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GridItemAdapter.MediaItemHolder) {

            if ((getItem(position) as ListItem.MediaItem).type ==
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            ) {
                holder.binding.ivPlayMediaItem.visibility = View.VISIBLE
            }

            holder.binding.image.transitionName = getItemId(position).toString()

            GlideApp.with(holder.binding.image)
                .load((getItem(position) as ListItem.MediaItem).uri)
                .signature(
                    MediaStoreSignature(
                        null,
                        (getItem(position) as ListItem.MediaItem).dateModified, 0
                    )
                )
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any,
                        target: Target<Drawable?>, isFirstResource: Boolean
                    ): Boolean {
                        if (MainActivity.currentListPosition != holder.layoutPosition) {
                            return true
                        }
                        if (enterTransitionStarted.getAndSet(true)) {
                            return true
                        }

                        frag.startPostponedEnterTransition()

                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (MainActivity.currentListPosition != holder.layoutPosition) {
                            return false
                        }
                        if (enterTransitionStarted.getAndSet(true)) {
                            return false
                        }

                        frag.startPostponedEnterTransition()

                        return false
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.binding.image)
/*
            holder.binding.image.setOnClickListener {


                MainActivity.currentListPosition = holder.layoutPosition
                MainActivity.currentViewPagerPosition = if (isAlbum) {
                    holder.layoutPosition
                } else {
                    (getItem(holder.layoutPosition) as ListItem.MediaItem).viewPagerPosition
                }

                FragmentNavigatorExtras(
                    it to it.transitionName
                )

            }

 */
        } else if (holder is GridItemAdapter.HeaderViewHolder) {
            holder.binding.tvDate.text =
                (getItem(position) as ListItem.Header).description
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.MediaItem -> (getItem(position) as ListItem.MediaItem).type
            is ListItem.Header -> GridItemAdapter.ITEM_VIEW_TYPE_HEADER
            else -> throw IllegalStateException("Unknown ViewType")
        }
    }

    private inner class MediaItemHolder(binding: ListGridMediaItemHolderBinding) :
        RecyclerView.ViewHolder(binding.root)

    private inner class HeaderViewHolder(binding: ListGridHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)
}