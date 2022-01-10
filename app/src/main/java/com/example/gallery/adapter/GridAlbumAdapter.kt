package com.example.gallery.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.signature.MediaStoreSignature
import com.example.gallery.Album
import com.example.gallery.GlideApp
import com.example.gallery.R
import com.example.gallery.databinding.AlbumHolderBinding
import com.example.gallery.ui.BottomNavFrag
import com.example.gallery.ui.MainActivity
import com.google.android.material.transition.MaterialSharedAxis

class GridAlbumAdapter(private val frag: BottomNavFrag): ListAdapter<Album, GridAlbumAdapter.AlbumHolder>(Album.DiffCallback) {

    inner class AlbumHolder(private val binding: AlbumHolderBinding): RecyclerView.ViewHolder(binding.root){
        fun onBind() {
            GlideApp.with(frag.requireActivity())
                .load(getItem(layoutPosition).mediaItems[0].uri)
                .signature(MediaStoreSignature("", getItem(layoutPosition).mediaItems[0].dateModified, 0))
                .thumbnail(0.3f)
                .into(binding.ivThumbnailAlbum)
            binding.tvAlbumName.text = getItem(layoutPosition).name
            binding.ivThumbnailAlbum.transitionName = "album_$layoutPosition"

            binding.ivThumbnailAlbum.setOnClickListener {
                try {
                    frag.exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
                    MainActivity.currentListPosition = 0
                    MainActivity.currentAlbumName = getItem(layoutPosition).name
                    frag.findNavController().navigate(
                        R.id.action_bottomNavFrag_to_albumDetailFrag,
                        null,
                        null,
                        null)
                } catch (e: java.lang.IllegalArgumentException){}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        return AlbumHolder(AlbumHolderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        holder.onBind()
    }
}