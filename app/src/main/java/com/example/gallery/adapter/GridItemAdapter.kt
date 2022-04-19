package com.example.gallery.adapter

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.example.gallery.GlideApp
import com.example.gallery.ListItem
import com.example.gallery.R
import com.example.gallery.databinding.ListGridHeaderBinding
import com.example.gallery.databinding.ListGridMediaItemHolderBinding
import com.example.gallery.ui.AlbumDetailFrag
import com.example.gallery.ui.BinFrag
import com.example.gallery.ui.BottomNavFrag
import com.example.gallery.ui.MainActivity
import com.google.android.material.shape.ShapeAppearanceModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Based on:
 * https://github.com/android/animation-samples/tree/main/GridToPager
 */
class GridItemAdapter(private val frag: Fragment, private val isAlbum: Boolean) :
    ListAdapter<ListItem, ViewHolder>(ListItem.ListItemDiffCallback()) {

    private val enterTransitionStarted: AtomicBoolean = AtomicBoolean()
    var tracker: SelectionTracker<Long>? = null

    companion object {
        const val ITEM_VIEW_TYPE_HEADER: Int = 8123
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> HeaderViewHolder(
                ListGridHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )

            else -> MediaItemHolder(
                ListGridMediaItemHolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                ),
                viewType
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is MediaItemHolder) {

            holder.binding.image.isActivated = tracker?.isSelected(getItemId(position)) == true

            if (holder.binding.image.isActivated) {

                holder.binding.image.apply {
                    shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(70f)
                    animate().scaleX(0.75f).scaleY(0.75f).duration = 100
                }

            } else if (!holder.binding.image.isActivated) {

                holder.binding.image.apply {
                    shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(0f)
                    animate().scaleX(1f).scaleY(1f).duration = 100
                }

            }

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

            holder.binding.image.setOnClickListener {

                if (frag.requireActivity().intent.action == Intent.ACTION_PICK ||
                    frag.requireActivity().intent.action == Intent.ACTION_GET_CONTENT
                ) {
                    if (!frag.requireActivity().intent.getBooleanExtra(
                            Intent.EXTRA_ALLOW_MULTIPLE,
                            false
                        )
                    ) {
                        val intent = Intent()
                        intent.data = (getItem(position) as ListItem.MediaItem).uri
                        frag.requireActivity().setResult(Activity.RESULT_OK, intent)
                        frag.requireActivity().finish()
                        return@setOnClickListener
                    } else {
                        tracker?.select(getItemId(position))
                        return@setOnClickListener
                    }
                }

                if (frag !is BinFrag) {
                    MainActivity.currentListPosition = position
                    MainActivity.currentViewPagerPosition = if (isAlbum) {
                        position
                    } else {
                        (getItem(position) as ListItem.MediaItem).viewPagerPosition
                    }
                }

                val extras = FragmentNavigatorExtras(it to it.transitionName)

                when (frag) {

                    is BottomNavFrag -> {
                        frag.setHoldTransition()
                        frag.prepareTransitions()
                        frag.findNavController().navigate(
                            R.id.action_bottomNavFrag_to_viewPagerFrag,
                            null,
                            null,
                            extras
                        )
                    }

                    is AlbumDetailFrag -> {
                        val args = Bundle()
                        args.putBoolean("isAlbum", true)
                        frag.findNavController().navigate(
                            R.id.action_albumDetailFrag_to_viewPagerFrag,
                            args,
                            null,
                            extras
                        )
                    }

                    is BinFrag -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val senderRequest = MediaStore.createTrashRequest(
                                frag.requireActivity().application.contentResolver,
                                listOf((getItem(position) as ListItem.MediaItem).uri), false
                            ).intentSender
                            val intentSenderRequest =
                                IntentSenderRequest.Builder(senderRequest).build()
                            (frag.requireActivity() as MainActivity).restoreRequest.launch(
                                intentSenderRequest
                            )
                        }
                    }

                }
            }
        } else if (holder is HeaderViewHolder) {
            holder.binding.tvDate.text =
                SimpleDateFormat.getDateInstance(SimpleDateFormat.FULL).format(
                    Date(getItemId(position))
                )
        }
    }


    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.MediaItem -> (getItem(position) as ListItem.MediaItem).type
            is ListItem.Header -> ITEM_VIEW_TYPE_HEADER
            else -> throw IllegalStateException("Unknown ViewType")
        }
    }

    inner class MediaItemHolder(val binding: ListGridMediaItemHolderBinding, val type: Int) :
        RecyclerView.ViewHolder(binding.root) {

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int =
                    layoutPosition

                override fun getSelectionKey(): Long =
                    getItem(layoutPosition).id
            }
    }

    inner class HeaderViewHolder(val binding: ListGridHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int =
                    layoutPosition

                override fun getSelectionKey(): Long =
                    getItem(layoutPosition).id
            }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id
}