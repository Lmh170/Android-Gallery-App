package com.example.gallery.adapter

import android.content.Intent
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
import com.google.android.material.navigation.NavigationBarView

class GridAlbumAdapter(private val frag: BottomNavFrag) : ListAdapter<Album,
        AlbumHolder>(Album.DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        return AlbumHolder(
            AlbumHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {

        GlideApp.with(holder.binding.ivThumbnailAlbum)
            .load(getItem(position).mediaItems[0].uri)
            .signature(
                MediaStoreSignature(
                    null, getItem(position)
                        .mediaItems[0].dateModified, 0
                )
            )
            .into(holder.binding.ivThumbnailAlbum)

        holder.binding.tvAlbumName.text = getItem(position).name
        holder.binding.ivThumbnailAlbum.transitionName = "album_$position"

        holder.binding.ivThumbnailAlbum.setOnClickListener {
            if ((frag.binding.bnvMain as NavigationBarView).selectedItemId == R.id.miAlbums
                || frag.requireActivity().intent.action == Intent.ACTION_PICK || frag.requireActivity()
                    .intent.action ==
                Intent.ACTION_GET_CONTENT
            ) {
                MainActivity.currentListPosition = 0

                MainActivity.currentAlbumName = getItem(position).name

                frag.setSharedAxisTransition()

                frag.findNavController().navigate(
                    R.id.action_bottomNavFrag_to_albumDetailFrag,
                    null,
                    null,
                    null
                )
            }
        }
    }
}

class AlbumHolder(val binding: AlbumHolderBinding) :
    RecyclerView.ViewHolder(binding.root)