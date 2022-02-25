package com.example.gallery.adapter

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.example.gallery.GlideApp
import com.example.gallery.ListItem
import com.example.gallery.databinding.ViewPagerItemHolderBinding
import com.example.gallery.ui.MainActivity
import com.example.gallery.ui.VideoPlayerActivity
import com.example.gallery.ui.ViewPagerFrag
import java.util.concurrent.atomic.AtomicBoolean

class ViewPagerAdapter(val frag: ViewPagerFrag): ListAdapter<ListItem.MediaItem, ViewPagerAdapter.ViewHolderPager>(ListItem.MediaItem.DiffCallback) {
    val enterTransitionStarted: AtomicBoolean = AtomicBoolean()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderPager {
        return  ViewHolderPager(
            ViewPagerItemHolderBinding.inflate(LayoutInflater.from(parent.context),
                parent, false)
        )
    }

    override fun onBindViewHolder(holderPager: ViewHolderPager, position: Int) {
        holderPager.onBind()
    }

    inner class ViewHolderPager(val binding: ViewPagerItemHolderBinding): RecyclerView.ViewHolder(binding.root) {
        fun onBind() {
            if ((getItem(layoutPosition) as ListItem.MediaItem).type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                binding.ivPlayButton.visibility = View.VISIBLE
                binding.ivPlayButton.setOnClickListener {
                    val args = Bundle()
                    args.putParcelable("videoUri", (getItem(layoutPosition) as ListItem.MediaItem).uri)
                  //  frag.findNavController().navigate(R.id.action_viewPagerFrag_to_videoPlayerFrag, args, null, null)
                    val intent = Intent(frag.context, VideoPlayerActivity::class.java).apply {
                        data = (getItem(layoutPosition) as ListItem.MediaItem).uri
                    }
                    frag.startActivity(intent)
                }
            } else {
                binding.pagerImage.enableZooming()
            }
            binding.pagerImage.transitionName = getItem(layoutPosition).id.toString()
            binding.pagerImage.gFrag = frag

            GlideApp.with(binding.pagerImage)
                .load(getItem(layoutPosition).uri)
                .signature(MediaStoreSignature(null, getItem(layoutPosition).dateModified, 0))
                .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (MainActivity.currentViewPagerPosition != layoutPosition) {
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
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (MainActivity.currentViewPagerPosition != layoutPosition) {
                        return false
                    }
                    if (enterTransitionStarted.getAndSet(true)) {
                        return false
                    }
                    frag.startPostponedEnterTransition()
                    return false
                }
                }).into(binding.pagerImage)
            binding.pagerImage.setOnClickListener { frag.toggleSystemUI() }
            binding.root.setOnClickListener { frag.toggleSystemUI() }
        }

    }
}