package com.example.gallery.ui

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.app.SharedElementCallback
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.TransitionManager
import com.example.gallery.ListItem
import com.example.gallery.MyItemDetailsLookup
import com.example.gallery.MyItemKeyProvider
import com.example.gallery.R
import com.example.gallery.adapter.GridAlbumAdapter
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentBottomNavBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import kotlin.collections.set

class BottomNavFrag : Fragment() {
    private lateinit var _binding: FragmentBottomNavBinding
    val binding: FragmentBottomNavBinding get() = _binding
    private val viewModel: MainViewModel by activityViewModels()
    var actionMode: ActionMode? = null
    private lateinit var tracker: SelectionTracker<Long>

    private val callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            activity?.menuInflater?.inflate(R.menu.contextual_action_bar, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean =
            false

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.miShare -> {
                    val items = mutableListOf<ListItem.MediaItem>()
                    for (id in tracker.selection) {
                        val selectedItem = viewModel.recyclerViewItems.value?.find {
                            it.id == id
                        } ?: return false
                        items.add(selectedItem as ListItem.MediaItem)
                    }
                    ViewPagerFrag.share(items, requireActivity())
                    tracker.clearSelection()
                    actionMode?.finish()
                    true
                }

                R.id.miDelete -> {
                    val items = mutableListOf<ListItem.MediaItem>()
                    for (id in tracker.selection) {
                        val selectedItem = viewModel.recyclerViewItems.value?.find {
                            it.id == id
                        } ?: return false
                        items.add(selectedItem as ListItem.MediaItem)
                    }
                    viewModel.deleteItems(items)
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
                    android.R.color.transparent,
                    requireActivity().theme
                )
            }, 400)
            actionMode = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.recyclerViewItems.observe(viewLifecycleOwner) { items ->
            val position = (binding.rvItems.layoutManager as GridLayoutManager)
                .findFirstCompletelyVisibleItemPosition()

            (binding.rvItems.adapter as GridItemAdapter).submitList(items) {
                if (position == 0) binding.rvItems.scrollToPosition(0)
            }
        }

        viewModel.albums.observe(viewLifecycleOwner) { items ->
            val position = (binding.rvAlbums.layoutManager as GridLayoutManager)
                .findFirstCompletelyVisibleItemPosition()

            (binding.rvAlbums.adapter as GridAlbumAdapter).submitList(items) {
                if (position == 0) binding.rvAlbums.scrollToPosition(0)
            }
        }

        if (::_binding.isInitialized) return binding.root

        _binding = FragmentBottomNavBinding.inflate(inflater, container, false)

        if (requireActivity().intent.action == Intent.ACTION_PICK ||
            requireActivity().intent.action == Intent.ACTION_GET_CONTENT
        ) {
            setUpForIntent()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                binding.tbMain.inflateMenu(R.menu.action_bar_home)
            }

            binding.tbMain.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.miTrash -> {
                        setSharedAxisTransition()
                        MainActivity.currentListPosition = Int.MIN_VALUE
                        findNavController().navigate(R.id.action_bottomNavFrag_to_binFrag)
                        return@setOnMenuItemClickListener true
                    }
                    else -> return@setOnMenuItemClickListener false
                }
            }
        }

        setUpViews()
        setUpNavigationView()
        prepareTransitions()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (binding.rvItems.isVisible && MainActivity.currentListPosition != Int.MIN_VALUE) {
            postponeEnterTransition()
            scrollToPosition()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::_binding.isInitialized) {
            outState.putBoolean(RV_ITEMS_VISIBILITY, binding.rvItems.isVisible)
            outState.putBoolean(RV_ALBUMS_VISIBILITY, binding.rvAlbums.isVisible)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            binding.rvItems.isVisible = savedInstanceState.getBoolean(RV_ITEMS_VISIBILITY)
            binding.rvAlbums.isVisible = savedInstanceState.getBoolean(RV_ALBUMS_VISIBILITY)
        }
    }

    private fun setUpViews() {
        binding.rvItems.apply {
            adapter = GridItemAdapter(this@BottomNavFrag, false) { extras, _ ->
                if ((binding.bnvMain as NavigationBarView).selectedItemId != R.id.miPhotos) return@GridItemAdapter
                setHoldTransition()
                prepareTransitions()
                findNavController().navigate(
                    R.id.action_bottomNavFrag_to_viewPagerFrag,
                    null,
                    null,
                    extras
                )
            }

            val manager = GridLayoutManager(context, resources.getInteger(R.integer.spanCount))

            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter?.getItemViewType(position)) {
                        GridItemAdapter.ITEM_VIEW_TYPE_HEADER -> resources.getInteger(R.integer.spanCount)
                        GridItemAdapter.ITEM_VIEW_TYPE_SEARCH -> resources.getInteger(R.integer.spanCount)
                        else -> 1
                    }
                }
            }

            layoutManager = manager
            setHasFixedSize(true)
        }

        tracker = SelectionTracker.Builder(
            "GritItemFragSelectionId",
            binding.rvItems,
            MyItemKeyProvider(viewModel),
            MyItemDetailsLookup(binding.rvItems),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {

            override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean =
                binding.rvItems.findViewHolderForItemId(key) is GridItemAdapter.MediaItemHolder

            override fun canSelectMultiple(): Boolean =
                true

            override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean =
                binding.rvItems.findViewHolderForLayoutPosition(position) is GridItemAdapter.MediaItemHolder

        }).build().apply {
            addObserver(object : SelectionTracker.SelectionObserver<Long>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()

                    actionMode?.title = selection.size().toString()

                    if (actionMode == null) {
                        activity?.window?.statusBarColor = SurfaceColors.getColorForElevation(
                            requireContext(), binding.appBarLayout.elevation
                        )
                        actionMode = binding.tbMain.startActionMode(callback)
                    }

                    if (selection.size() == 0) {
                        actionMode?.finish()
                    }
                }
            })
        }

        (binding.rvItems.adapter as GridItemAdapter).tracker = tracker

        binding.rvAlbums.apply {
            adapter = GridAlbumAdapter(this@BottomNavFrag)
            setHasFixedSize(true)
            layoutManager =
                GridLayoutManager(context, resources.getInteger(R.integer.spanCount).div(2))
        }
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

    private fun setUpNavigationView() {
        binding.appBarLayout.statusBarForeground = MaterialShapeDrawable
            .createWithElevationOverlay(binding.appBarLayout.context)

        if (binding.bnvMain is BottomNavigationView) {
            binding.bnvMain.viewTreeObserver.addOnGlobalLayoutListener {

                binding.rvItems.updatePadding(bottom = binding.bnvMain.height)
                binding.rvAlbums.updatePadding(bottom = binding.bnvMain.height)

            }
        } else {
            binding.tbMain.viewTreeObserver.addOnGlobalLayoutListener {
                binding.bnvMain.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = binding.appBarLayout.height
                }
            }

            binding.bnvMain.viewTreeObserver.addOnGlobalLayoutListener {

                binding.rvItems.updatePadding(left = binding.bnvMain.width)
                binding.rvAlbums.updatePadding(left = binding.bnvMain.width)

            }
        }

        (binding.bnvMain as NavigationBarView).setOnItemSelectedListener {
            when (it.itemId) {
                R.id.miPhotos -> {
                    TransitionManager.beginDelayedTransition(
                        binding.root,
                        MaterialFadeThrough().apply {
                            excludeTarget(binding.bnvMain, true)
                        })

                    binding.rvItems.isTransitionGroup = true
                    binding.rvAlbums.isVisible = false
                    binding.rvItems.isVisible = true
                    binding.appBarLayout.setExpanded(true)
                    true
                }

                R.id.miAlbums -> {
                    TransitionManager.beginDelayedTransition(
                        binding.root,
                        MaterialFadeThrough().apply {
                            excludeTarget(binding.bnvMain, true)
                        })

                    binding.rvItems.isTransitionGroup = true
                    actionMode?.finish()
                    actionMode = null
                    binding.rvItems.isVisible = false
                    binding.rvAlbums.isVisible = true
                    binding.appBarLayout.setExpanded(true)
                    true
                }

                else -> false
            }
        }

        (binding.bnvMain as NavigationBarView).setOnItemReselectedListener {
            when (it.itemId) {
                R.id.miPhotos -> {
                    binding.rvItems.scrollToPosition(0)
                }

                R.id.miAlbums -> {
                    binding.rvAlbums.scrollToPosition(0)
                }
            }

            binding.appBarLayout.setExpanded(true)
        }
    }

    private fun setUpForIntent() {
        binding.tbMain.isTitleCentered = false

        binding.tbMain.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.tbMain.setNavigationIconTint(
            resources.getColor(
                android.R.color.black,
                activity?.theme
            )
        )

        if (!requireActivity().intent.getBooleanExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                false
            )
        ) {
            binding.tbMain.title = getString(R.string.select_single_item)
        } else {
            binding.tbMain.title = getString(R.string.select_multiple_items)
        }

        binding.bnvMain.visibility = View.GONE
        binding.rvItems.isVisible = false
        binding.rvAlbums.isVisible = true

        binding.tbMain.setNavigationOnClickListener {
            requireActivity().setResult(Activity.RESULT_CANCELED)
            requireActivity().finish()
        }
    }

    override fun onStart() {
        super.onStart()
        setUpSystemBars()
    }

    fun setSharedAxisTransition() {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    private fun setHoldTransition() {
        exitTransition = Hold()
        reenterTransition = Hold()
    }

    private fun prepareTransitions() {
        setExitSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {

                    if ((binding.rvItems.layoutManager as GridLayoutManager)
                            .findFirstCompletelyVisibleItemPosition() != 0
                    ) {
                        binding.appBarLayout.setExpanded(false, false)
                    }

                    binding.rvItems.isTransitionGroup = false

                    val selectedViewHolder = binding.rvItems
                        .findViewHolderForLayoutPosition(MainActivity.currentListPosition) ?: return

                    sharedElements[names[0]] =
                        (selectedViewHolder as GridItemAdapter.MediaItemHolder).binding.image
                }
            }
        )
    }

    private fun scrollToPosition() {
        binding.rvItems.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
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
                binding.rvItems.removeOnLayoutChangeListener(this)

                val viewAtPosition =
                    binding.rvItems.layoutManager!!.findViewByPosition(MainActivity.currentListPosition)

                // Scroll to position if the view for the current position is null (not currently part of
                // layout manager children), or it's not completely visible.
                if (viewAtPosition == null || !binding.rvItems.layoutManager!!
                        .isViewPartiallyVisible(viewAtPosition, true, true)
                ) {
                    binding.rvItems.post {
                        binding.rvItems.layoutManager!!.scrollToPosition(MainActivity.currentListPosition)
                        startPostponedEnterTransition()
                    }
                } else {
                    startPostponedEnterTransition()
                }
            }
        })
    }

    companion object {
        const val RV_ITEMS_VISIBILITY: String = "rv_items_visibility"
        const val RV_ALBUMS_VISIBILITY: String = "rv_albums_visibility"
    }
}