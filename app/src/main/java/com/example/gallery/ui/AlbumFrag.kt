package com.example.gallery.ui

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.ListItem
import com.example.gallery.R
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentAlbumBinding
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis

open class AlbumFrag : MediaFrag() {
    private lateinit var _binding: FragmentAlbumBinding
    protected val binding: FragmentAlbumBinding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.albums.observe(viewLifecycleOwner) { albums ->
            (binding.rvAlbumDetail.adapter as GridItemAdapter).submitList(albums.find {
                it.name == arguments?.getString(
                    "currentAlbumName"
                )
            }?.mediaItems as List<ListItem>?) {
                scrollToFirst(binding.rvAlbumDetail)
            }
        }

        setUpAlbumFrag(
            inflater,
            container,
            R.id.action_albumFrag_to_viewPagerFrag
        )

        if (requireActivity().intent.getBooleanExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                false
            )
        ) {
            binding.fabDone.show()
            binding.fabDone.setOnClickListener {
                val intent = Intent()
                val items = mutableListOf<Uri>()

                for (key in (binding.rvAlbumDetail.adapter as GridItemAdapter).tracker!!.selection) {
                    viewModel.albums.value?.find {
                        it.name == arguments?.getString("currentAlbumName")
                    }?.mediaItems?.find {
                        it.id == key
                    }?.uri?.let { it1 -> items.add(it1) }
                }

                intent.clipData = ClipData.newUri(
                    requireActivity().contentResolver,
                    "uris",
                    items[0]
                )

                for (i in 1 until items.size) {
                    intent.clipData?.addItem(ClipData.Item(items[i]))
                }

                if (items.size == 1) {
                    intent.putExtra(Intent.EXTRA_STREAM, items[0] as Parcelable)
                } else {
                    intent.putParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        items as ArrayList<out Parcelable>
                    )
                }

                requireActivity().setResult(Activity.RESULT_OK, intent)
                requireActivity().finish()
            }
        }

        binding.tbAlbum.title = arguments?.getString("currentAlbumName")

        binding.tbAlbum.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

    protected fun setUpAlbumFrag(
        inflater: LayoutInflater,
        container: ViewGroup?,
        @IdRes resId: Int
    ) {
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        exitTransition = Hold()

        if (::_binding.isInitialized) return

        _binding = FragmentAlbumBinding.inflate(inflater, container, false)

        prepareTransitions(
            binding.rvAlbumDetail
        )

        binding.rvAlbumDetail.apply {
            adapter = GridItemAdapter(this@AlbumFrag) { extras, position ->
                MainActivity.currentListPosition = position
                MainActivity.currentViewPagerPosition = if (this@AlbumFrag is SearchResultsFrag) {
                    (viewModel.recyclerViewItems.value?.get(position) as ListItem.MediaItem).viewPagerPosition
                } else {
                    position
                }

                findNavController().navigate(
                    resId,
                    Bundle().apply {
                        putString("currentAlbumName", arguments?.getString("currentAlbumName"))
                    },
                    null,
                    extras
                )
            }
            setHasFixedSize(true)
        }

        setUpRecyclerViewSelection(
            binding.rvAlbumDetail,
            "AlbumFragSelection"
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?): Unit =
        onViewCreated(view, savedInstanceState, binding.rvAlbumDetail)
}