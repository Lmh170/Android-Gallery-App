package com.example.gallery.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.MediaStoreSignature
import com.example.gallery.Album
import com.example.gallery.GlideApp
import com.example.gallery.R
import com.example.gallery.databinding.AlbumHolderBinding
import com.example.gallery.ui.BottomNavFrag
import com.example.gallery.ui.MainActivity
import com.google.android.material.transition.MaterialSharedAxis

class GridAlbumAdapter(private val frag: BottomNavFrag): ListAdapter<Album,
        GridAlbumAdapter.AlbumHolder>(Album.DiffCallback) {

    inner class AlbumHolder(private val binding: AlbumHolderBinding):
        RecyclerView.ViewHolder(binding.root){
        fun onBind() {
            GlideApp.with(binding.ivThumbnailAlbum)
                .load(getItem(layoutPosition).mediaItems[0].uri)
                .apply(RequestOptions().format(DecodeFormat.PREFER_RGB_565)) // better performance
                .signature(MediaStoreSignature(null, getItem(layoutPosition)
                    .mediaItems[0].dateModified, 0))
                .into(binding.ivThumbnailAlbum)

            binding.tvAlbumName.text = getItem(layoutPosition).name
            binding.ivThumbnailAlbum.transitionName = "album_$layoutPosition"

            binding.ivThumbnailAlbum.setOnClickListener {
                frag.exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
                frag.enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)

                MainActivity.currentListPosition = 0
                MainActivity.currentAlbumName = getItem(layoutPosition).name
                frag.setSharedAxisTransition()
                frag.findNavController().navigate(
                    R.id.action_bottomNavFrag_to_albumDetailFrag,
                    null,
                    null,
                    null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        return AlbumHolder(AlbumHolderBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        holder.onBind()
    }
}