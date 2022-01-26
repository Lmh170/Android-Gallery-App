/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.gallery.adapter

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.example.gallery.ListItem
import com.example.gallery.R
import com.example.gallery.databinding.ListGridHeaderBinding
import com.example.gallery.databinding.ListGridMediaItemHolderBinding
import com.example.gallery.ui.AlbumDetailFrag
import com.example.gallery.ui.BottomNavFrag
import com.example.gallery.ui.MainActivity
import com.google.android.material.shape.ShapeAppearanceModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class GridItemAdapter(val frag: Fragment, val isAlbum: Boolean): ListAdapter<ListItem, ViewHolder>(ListItem.ListItemDiffCallback()) {
    private val enterTransitionStarted: AtomicBoolean = AtomicBoolean()
    lateinit var tracker: SelectionTracker<Long>

    companion object {
        const val ITEM_VIEW_TYPE_HEADER = 8123
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {
                MediaItemHolder(
                    ListGridMediaItemHolderBinding.inflate(LayoutInflater.from(parent.context),
                        parent, false), viewType)
            }
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                MediaItemHolder(
                    ListGridMediaItemHolderBinding.inflate(LayoutInflater.from(parent.context),
                        parent, false), viewType)

            }
            ITEM_VIEW_TYPE_HEADER -> {
                HeaderViewHolder(
                    ListGridHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is MediaItemHolder) {
            holder.onBind()
        } else if (holder is HeaderViewHolder) {
            holder.onBind()
        }
    }


    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.MediaItem -> (getItem(position) as ListItem.MediaItem).type
            is ListItem.Header -> ITEM_VIEW_TYPE_HEADER
            else -> 0
        }
    }

    inner class MediaItemHolder(val binding: ListGridMediaItemHolderBinding, val type: Int): RecyclerView.ViewHolder(binding.root) {
        fun getItemDetails() : ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int =
                    layoutPosition

                override fun getSelectionKey(): Long =
                    itemId
            }

        fun onBind() {
            binding.image.isActivated = tracker.isSelected(itemId)
            if (binding.image.isActivated) {
                binding.image.shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(70f)
            } else {
                binding.image.shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(0f)
            }
            if ((getItem(layoutPosition) as ListItem.MediaItem).type ==
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    binding.ivPlayMediaItem.visibility = View.VISIBLE
            }
            binding.image.transitionName = (getItem(layoutPosition) as ListItem.MediaItem).id.toString()

            Glide.with(binding.image).
                load((getItem(layoutPosition) as ListItem.MediaItem).uri)
                .centerCrop()
                .signature(MediaStoreSignature(null,
                    (getItem(layoutPosition) as ListItem.MediaItem).dateModified, 0))
              //  .thumbnail(0.2f)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any,
                        target: Target<Drawable?>, isFirstResource: Boolean
                    ): Boolean {
                        if (MainActivity.currentListPosition != layoutPosition) {
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
                        if (MainActivity.currentListPosition != layoutPosition) {
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
                .into(binding.image)

            binding.image.setOnClickListener {
                MainActivity.currentListPosition = layoutPosition
                MainActivity.currentViewPagerPosition = if (isAlbum){
                    layoutPosition
                } else {
                    (getItem(layoutPosition) as ListItem.MediaItem).viewPagerPosition
                }

                val extras = FragmentNavigatorExtras(it to it.transitionName)
                if (frag is BottomNavFrag) {
                    BottomNavFrag.enteringFromAlbum = false
                    frag.prepareTransitions()
                    frag.findNavController().navigate(
                        R.id.action_bottomNavFrag_to_viewPagerFrag,
                        null,
                        null,
                        extras)
                } else if (frag is AlbumDetailFrag) {
                    val args = Bundle()
                    args.putBoolean("isAlbum", true)
                    frag.findNavController().navigate(
                        R.id.action_albumDetailFrag_to_viewPagerFrag,
                        args,
                        null,
                        extras)


                }
            }
        }
    }

    inner class HeaderViewHolder (private val binding: ListGridHeaderBinding): RecyclerView.ViewHolder(binding.root) {
        fun onBind() {
            binding.tvDate.text = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG).format(
                Date(itemId)
            )
        }

        fun getItemDetails() : ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int =
                    layoutPosition

                override fun getSelectionKey(): Long =
                    itemId
            }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id
}