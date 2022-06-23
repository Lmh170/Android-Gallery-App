package com.example.gallery.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.TransitionManager
import com.example.gallery.ListItem
import com.example.gallery.R
import com.example.gallery.adapter.GridAlbumAdapter
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentBottomNavBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis

class BottomNavFrag : MediaFrag() {
    private lateinit var _binding: FragmentBottomNavBinding
    val binding: FragmentBottomNavBinding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.recyclerViewItems.observe(viewLifecycleOwner) { items ->
            (binding.rvItems.adapter as GridItemAdapter).submitList(items) {
                scrollToFirst(binding.rvItems)
            }
        }

        viewModel.albums.observe(viewLifecycleOwner) { items ->
            (binding.rvAlbums.adapter as GridAlbumAdapter).submitList(items) {
                scrollToFirst(binding.rvAlbums)
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
                binding.tbMain.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.miTrash -> {
                            setSharedAxisTransition()
                            MainActivity.currentListPosition = Int.MIN_VALUE
                            findNavController().navigate(
                                R.id.action_bottomNavFrag_to_binFrag,
                                Bundle().apply {
                                    putString("currentAlbumName", binFragID)
                                }
                            )
                            return@setOnMenuItemClickListener true
                        }
                        else -> return@setOnMenuItemClickListener false
                    }
                }
            }
        }

        setUpViews()
        setUpNavigationView()
        prepareTransitions(
            binding.rvItems,
            binding.appBarLayout
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (binding.rvItems.isVisible && MainActivity.currentListPosition != Int.MIN_VALUE) {
            onViewCreated(view, savedInstanceState, binding.rvItems)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::_binding.isInitialized) {
            outState.putBoolean(RV_ITEMS_VISIBILITY, binding.rvItems.isVisible)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            binding.rvItems.isVisible = savedInstanceState.getBoolean(RV_ITEMS_VISIBILITY)
            binding.rvAlbums.isVisible = !savedInstanceState.getBoolean(RV_ITEMS_VISIBILITY)
        }
    }

    private fun setUpViews() {
        binding.rvItems.apply {
            adapter = GridItemAdapter(this@BottomNavFrag) { extras, position ->
                MainActivity.currentListPosition = position
                MainActivity.currentViewPagerPosition =
                    (viewModel.recyclerViewItems.value?.get(position) as ListItem.MediaItem).viewPagerPosition

                if ((binding.bnvMain as NavigationBarView).selectedItemId != R.id.miPhotos) return@GridItemAdapter

                setHoldTransition()

                findNavController().navigate(
                    R.id.action_bottomNavFrag_to_viewPagerFrag,
                    null,
                    null,
                    extras
                )
            }

            layoutManager =
                GridLayoutManager(context, resources.getInteger(R.integer.spanCount)).apply {
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when (adapter?.getItemViewType(position)) {
                                GridItemAdapter.ITEM_VIEW_TYPE_HEADER -> resources.getInteger(R.integer.spanCount)
                                GridItemAdapter.ITEM_VIEW_TYPE_SEARCH -> resources.getInteger(R.integer.spanCount)
                                else -> 1
                            }
                        }
                    }
                }
            setHasFixedSize(true)
        }

        setUpRecyclerViewSelection(
            binding.rvItems,
            "BottomNavFragSelection"
        )

        binding.rvAlbums.apply {
            adapter = GridAlbumAdapter(this@BottomNavFrag)
            setHasFixedSize(true)
            layoutManager =
                GridLayoutManager(context, resources.getInteger(R.integer.spanCount).div(2))
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

        TypedValue().also {
            requireActivity().theme.resolveAttribute(R.attr.colorOnSurface, it, true)

            binding.tbMain.setNavigationIconTint(
                it.data
            )
        }

        if (!requireActivity().intent.getBooleanExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                false
            )
        ) {
            binding.tbMain.title = getString(R.string.select_single_item)
        } else {
            binding.tbMain.title = getString(R.string.select_multiple_items)
        }

        binding.bnvMain.isVisible = false
        binding.rvItems.isVisible = false
        binding.rvAlbums.isVisible = true

        binding.tbMain.setNavigationOnClickListener {
            requireActivity().setResult(Activity.RESULT_CANCELED)
            requireActivity().finish()
        }
    }

    fun setSharedAxisTransition() {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    private fun setHoldTransition() {
        exitTransition = Hold()
        reenterTransition = Hold()
    }

    companion object {
        const val RV_ITEMS_VISIBILITY: String = "rv_items_visibility"
    }
}