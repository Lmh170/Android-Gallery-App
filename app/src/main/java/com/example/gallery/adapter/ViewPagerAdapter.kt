package com.example.gallery.adapter

import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
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
import com.example.gallery.R
import com.example.gallery.databinding.ViewPagerItemHolderBinding
import com.example.gallery.ui.MainActivity
import com.example.gallery.ui.VideoPlayerActivity
import com.example.gallery.ui.ViewPagerFrag
import java.util.concurrent.atomic.AtomicBoolean

class ViewPagerAdapter(val frag: ViewPagerFrag) : ListAdapter<ListItem.MediaItem,
        ViewHolderPager>(ListItem.MediaItem.DiffCallback) {

    private val enterTransitionStarted: AtomicBoolean = AtomicBoolean()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderPager {
        return ViewHolderPager(
            ViewPagerItemHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holderPager: ViewHolderPager, position: Int) {
        if ((getItem(position) as ListItem.MediaItem).type ==
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        ) {
            holderPager.binding.ivPlayButton.isVisible = true

            holderPager.binding.ivPlayButton.setOnClickListener {
                Intent(frag.context, VideoPlayerActivity::class.java).apply {
                    data = (getItem(holderPager.layoutPosition) as ListItem.MediaItem).uri
                }.also {
                    frag.startActivity(it)
                }
            }
        } else {
            holderPager.binding.ivPlayButton.isVisible = false
            holderPager.binding.pagerImage.enableZooming()
        }

        holderPager.binding.pagerImage.transitionName =
            getItem(holderPager.layoutPosition).id.toString()

        holderPager.binding.pagerImage.gFrag = frag

        GlideApp.with(holderPager.binding.pagerImage)
            .load(getItem(position).uri)
            .error(R.drawable.ic_baseline_image_not_supported_24)
            .centerInside()
            .signature(
                MediaStoreSignature(
                    null,
                    getItem(position).dateModified,
                    0
                )
            )
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (MainActivity.currentViewPagerPosition != holderPager.layoutPosition) {
                        return false
                    }
                    if (enterTransitionStarted.getAndSet(true)) {
                        return false
                    }

                    frag.startPostponedEnterTransition()

                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (MainActivity.currentViewPagerPosition != holderPager.layoutPosition) {
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
            .into(holderPager.binding.pagerImage)

        holderPager.binding.apply {
            pagerImage.setOnClickListener { frag.toggleSystemUI() }
            root.setOnClickListener { frag.toggleSystemUI() }
        }
    }
}

class ViewHolderPager(val binding: ViewPagerItemHolderBinding) :
    RecyclerView.ViewHolder(binding.root)