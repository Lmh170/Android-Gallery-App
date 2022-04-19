package com.example.gallery.ui

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentAlbumDetailBinding
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import com.example.gallery.ListItem
import com.example.gallery.MyItemDetailsLookup
import com.example.gallery.MyItemKeyProvider
import com.example.gallery.R
import com.google.android.material.elevation.SurfaceColors

class AlbumDetailFrag : Fragment() {
    private lateinit var _binding: FragmentAlbumDetailBinding
    private val binding get() = _binding
    private val viewModel: MainViewModel by activityViewModels()
    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.albums.observe(viewLifecycleOwner) { albums ->
            val items = albums.find { it.name == MainActivity.currentAlbumName }?.mediaItems
            val position = (binding.rvAlbumDetail.layoutManager as GridLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
            (binding.rvAlbumDetail.adapter as GridItemAdapter).submitList(items as List<ListItem>?) {
                if (position == 0) binding.rvAlbumDetail.scrollToPosition(0)
            }
        }
        if (::_binding.isInitialized) return binding.root

        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)

        val adapter = GridItemAdapter(this@AlbumDetailFrag, true)
        binding.rvAlbumDetail.apply {
            this.adapter = adapter
            setHasFixedSize(true)
        }

        if (requireActivity().intent.action == Intent.ACTION_PICK || requireActivity().intent.action ==
            Intent.ACTION_GET_CONTENT &&
            requireActivity().intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false) ||
            requireActivity().intent.action == Intent.ACTION_MAIN
        ) {
            setUpRecyclerViewSelection()
        }

        binding.tbAlbum.title = MainActivity.currentAlbumName

        binding.tbAlbum.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        prepareTransitions()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        setUpSystemBars()
        scrollToPosition()
    }

    private fun setUpRecyclerViewSelection() {
        val tracker = SelectionTracker.Builder(
            "GritItemFragSelectionId",
            binding.rvAlbumDetail,
            MyItemKeyProvider(viewModel, true),
            MyItemDetailsLookup(binding.rvAlbumDetail),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {
            override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean =
                binding.rvAlbumDetail.findViewHolderForItemId(key) != null

            override fun canSelectMultiple(): Boolean =
                true

            override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean =
                binding.rvAlbumDetail.findViewHolderForLayoutPosition(position) != null

        }).build()

        (binding.rvAlbumDetail.adapter as GridItemAdapter).tracker = tracker

        val callback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                if (requireActivity().intent.getBooleanExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        false
                    )
                ) {
                    binding.fabDone.show()
                    binding.fabDone.setOnClickListener {
                        val intent = Intent()
                        val items = mutableListOf<Uri>()
                        for (key in tracker.selection) {
                            viewModel.albums.value?.find {
                                it.name == MainActivity.currentAlbumName
                            }?.mediaItems?.find {
                                it.id == key
                            }?.uri?.let { it1 -> items.add(it1) }
                        }
                        intent.clipData = ClipData.newUri(
                            requireActivity().contentResolver,
                            "uris", items[0]
                        )
                        for (i in 1 until items.size) {
                            intent.clipData?.addItem(ClipData.Item(items[i]))
                        }
                        requireActivity().setResult(Activity.RESULT_OK, intent)
                        requireActivity().finish()
                    }
                    return true
                }
                activity?.menuInflater?.inflate(R.menu.contextual_action_bar, menu)
                activity?.window?.statusBarColor = SurfaceColors.getColorForElevation(
                    requireContext(), binding.appBarLayout.elevation
                )
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.miShare -> {
                        val items = mutableListOf<ListItem.MediaItem>()
                        for (id in tracker.selection) {
                            val selectedItem = (binding.rvAlbumDetail.adapter as GridItemAdapter)
                                .currentList.find {
                                    it.id == id
                                } as ListItem.MediaItem? ?: return false
                            items.add(selectedItem)
                        }
                        ViewPagerFrag.share(items, requireActivity())
                        tracker.clearSelection()
                        actionMode?.finish()
                        true
                    }
                    R.id.miDelete -> {
                        val items = mutableListOf<ListItem.MediaItem>()
                        for (id in tracker.selection) {
                            val selectedItem = (binding.rvAlbumDetail.adapter as GridItemAdapter)
                                .currentList.find {
                                    it.id == id
                                } as ListItem.MediaItem? ?: return false
                            items.add(selectedItem)
                        }
                        ViewPagerFrag.delete(items, requireContext(), viewModel)
                        actionMode?.finish()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                tracker.clearSelection()
                Handler(Looper.getMainLooper()).postDelayed({
                    activity?.window?.statusBarColor = resources.getColor(
                        android.R.color.transparent, requireActivity().theme
                    )
                }, 400)
                if (requireActivity().intent.getBooleanExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        false
                    )
                ) {
                    binding.fabDone.hide()
                }
                actionMode = null
            }
        }

        tracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                super.onSelectionChanged()
                actionMode?.title = tracker.selection.size().toString()
                if (actionMode == null) {
                    actionMode = binding.tbAlbum.startActionMode(callback)
                } else if (tracker.selection.size() == 0) {
                    actionMode?.finish()
                }
            }
        })
    }

    private fun scrollToPosition() {
        binding.rvAlbumDetail.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                binding.rvAlbumDetail.removeOnLayoutChangeListener(this)

                val viewAtPosition =
                    binding.rvAlbumDetail.layoutManager!!.findViewByPosition(MainActivity.currentListPosition)

                if (viewAtPosition == null || !binding.rvAlbumDetail.layoutManager!!
                        .isViewPartiallyVisible(viewAtPosition, true, true)
                ) {
                    binding.rvAlbumDetail.post {
                        binding.rvAlbumDetail.layoutManager!!.scrollToPosition(MainActivity.currentListPosition)
                        startPostponedEnterTransition()
                    }
                } else {
                    startPostponedEnterTransition()
                }
            }
        })
    }

    private fun prepareTransitions() {
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        exitTransition = Hold()
        setExitSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {

                    // Locate the ViewHolder for the clicked position.
                    val selectedViewHolder = binding.rvAlbumDetail
                        .findViewHolderForLayoutPosition(MainActivity.currentListPosition) ?: return
//                    (exitTransition as Hold).excludeChildren((selectedViewHolder as GridAdapter.MediaItemHolder).binding.image, true)

                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] =
                        (selectedViewHolder as GridItemAdapter.MediaItemHolder).binding.image

                }
            }
        )
    }

    private fun setUpSystemBars() {
        val nightModeFlags: Int =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO ||
            nightModeFlags == Configuration.UI_MODE_NIGHT_UNDEFINED
        ) {
            WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }

    }
}