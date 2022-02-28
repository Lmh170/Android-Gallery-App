package com.example.gallery.ui

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.ListItem
import com.example.gallery.MyItemDetailsLookup
import com.example.gallery.MyItemKeyProvider
import com.example.gallery.R
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentGridItemBinding
import com.google.android.material.transition.MaterialFadeThrough

class GridItemFrag : Fragment() {
    private lateinit var _binding: FragmentGridItemBinding
    val binding get() = _binding
    private val viewModel: MainViewModel by activityViewModels()
    var actionMode: ActionMode? = null
    private lateinit var tracker: SelectionTracker<Long>

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
        if (::_binding.isInitialized) {
            (binding.rvItems.adapter as GridItemAdapter).enterTransitionStarted.set(false)
            binding.rvItems.apply {
                val manager = GridLayoutManager(context, resources.getInteger(R.integer.spanCount))
                manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter?.getItemViewType(position)) {
                            GridItemAdapter.ITEM_VIEW_TYPE_HEADER -> resources.getInteger(R.integer.spanCount)
                            else -> 1
                        }
                    }
                }
                layoutManager = manager
                setHasFixedSize(true)
            }
        } else {
            _binding = FragmentGridItemBinding.inflate(inflater, container, false)
            setUpRecyclerView()
        }

        binding.rvItems.isTransitionGroup = true
        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()
        scrollToPosition()
        return binding.root
    }

    private fun setUpRecyclerView() {
        binding.rvItems.apply {
            this.adapter = GridItemAdapter(requireParentFragment(), false)
            val manager = GridLayoutManager(context, resources.getInteger(R.integer.spanCount))
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter?.getItemViewType(position)) {
                        GridItemAdapter.ITEM_VIEW_TYPE_HEADER -> resources.getInteger(R.integer.spanCount)
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
                binding.rvItems.findViewHolderForItemId(key) !is GridItemAdapter.HeaderViewHolder &&
                        binding.rvItems.findViewHolderForItemId(key) != null

            override fun canSelectMultiple(): Boolean =
                true

            override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean =
                binding.rvItems.findViewHolderForLayoutPosition(position) !is GridItemAdapter.HeaderViewHolder &&
                        binding.rvItems.findViewHolderForLayoutPosition(position) != null

        }).build()
        (binding.rvItems.adapter as GridItemAdapter).tracker = tracker
        val callback = object : ActionMode.Callback {
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
                        // Handle delete icon press
                        val items = mutableListOf<ListItem.MediaItem>()
                        for (id in tracker.selection) {
                            val selectedItem = viewModel.recyclerViewItems.value?.find {
                                it.id == id
                            } ?: return false
                            items.add(selectedItem as ListItem.MediaItem)
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        activity?.window?.statusBarColor = resources.getColor(android.R.color.transparent,
                            requireActivity().theme)
                    }
                }, 400)
                actionMode = null
            }
        }
        tracker.addObserver(object: SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                super.onSelectionChanged()
                actionMode?.title = tracker.selection.size().toString()
                if (actionMode == null) {
                    actionMode = (parentFragment as BottomNavFrag).startActionMode(callback)
                }
                if (tracker.selection.size() == 0) {
                    actionMode?.finish()
                }
            }
        })
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

                // val layoutManager = recyclerView.layoutManager
                val viewAtPosition =
                    binding.rvItems.layoutManager!!.findViewByPosition(MainActivity.currentListPosition)

                // Scroll to position if the view for the current position is null (not currently part of
                // layout manager children), or it's not completely visible.
                if (viewAtPosition == null || !binding.rvItems.layoutManager!!
                        .isViewPartiallyVisible(viewAtPosition, true, true)
                ) {
                    binding.rvItems.post {
                        binding.rvItems.layoutManager!!.scrollToPosition(MainActivity.currentListPosition)
                    }
                }
            }
        })
    }
}