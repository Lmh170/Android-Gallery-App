package com.example.gallery.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
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
import com.example.gallery.ui.ViewPagerFrag
import java.util.concurrent.atomic.AtomicBoolean

class ViewPagerAdapter(val frag: ViewPagerFrag): ListAdapter<ListItem.MediaItem, ViewPagerAdapter.ViewHolder>(ListItem.MediaItem.DiffCallback) {
    val transitionStarted: AtomicBoolean = AtomicBoolean()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return  ViewHolder(
            ViewPagerItemHolderBinding.inflate(LayoutInflater.from(parent.context),
                parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind()
    }

    inner class ViewHolder(val binding: ViewPagerItemHolderBinding): RecyclerView.ViewHolder(binding.root) {
        fun onBind() {
            binding.pagerImage.transitionName = getItem(layoutPosition).id.toString()
            binding.pagerImage.enableZooming()
            binding.pagerImage.gFrag = frag
            GlideApp.with(frag.requireActivity())
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
                    if (transitionStarted.getAndSet(true)) {
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
                    if (transitionStarted.getAndSet(true)) {
                        return false
                    }
                    frag.startPostponedEnterTransition()
                    return false
                }
                }).into(binding.pagerImage)
/*
                .into(object: CustomViewTarget<ZoomableImageView, Drawable>(binding.pagerImage) {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        TODO("Not yet implemented")
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        binding.pagerImage.updateLayoutParams<FrameLayout.LayoutParams> {
                            this.height = resource.intrinsicHeight
                        }
               //         myGraphView.setLayoutParams(LayoutParams(width, height))
                        TODO("Not yet implemented")
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {
                        TODO("Not yet implemented")
                    }


                })
                    /*
                .into( SimpleTarget<ImageView, Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                        Transition<? super Bitmap> transition) {
                        int w = bitmap.getWidth();
                        int h = bitmap.getHeight()
                        mImageView.setImageBitmap(bitmap);
                    }
                });

 */

                     */


            binding.pagerImage.setOnClickListener {
               frag.toggleSystemUI()
            }
            binding.root.setOnClickListener { frag.toggleSystemUI() }
        }

    }
}