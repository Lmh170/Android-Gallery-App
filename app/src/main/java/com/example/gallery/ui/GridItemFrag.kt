package com.example.gallery.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.MyItemDetailsLookup
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentGridItemBinding
import com.google.android.material.transition.MaterialFadeThrough

class GridItemFrag : Fragment() {
    private lateinit var _binding: FragmentGridItemBinding
    val binding get() = _binding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::_binding.isInitialized){
            _binding = FragmentGridItemBinding.inflate(inflater, container, false)
        }

        viewModel.recyclerViewItems.observe(viewLifecycleOwner, { items ->
            val position = (binding.rvItems.layoutManager as GridLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
            (binding.root.adapter as GridItemAdapter).submitList(items){
                if (position == 0) binding.rvItems.scrollToPosition(0)
            }
           }
        )
        val adapter = GridItemAdapter(requireParentFragment(), false)
        adapter.setHasStableIds(true)

        binding.root.apply {
            this.adapter = adapter
            val manager = GridLayoutManager(context, spanCount)
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.getItemViewType(position)) {
                        GridItemAdapter.ITEM_VIEW_TYPE_HEADER -> spanCount
                        else -> 1
                    }
                }
            }
            layoutManager = manager
            setHasFixedSize(true)

        }
        val tracker = SelectionTracker.Builder(
            "GritItemFragSelectionId",
            binding.rvItems,
            StableIdKeyProvider(binding.rvItems),
            MyItemDetailsLookup(binding.rvItems),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {
            override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean =
                key != Long.MIN_VALUE

            override fun canSelectMultiple(): Boolean =
                true

            override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean =
                true

        }).build()
        adapter.tracker = tracker
        scrollToPosition()
        ViewGroupCompat.setTransitionGroup(binding.rvItems, true)

        val callback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                // menuInflater.inflate(R.menu.contextual_action_bar, menu)
                //    WindowCompat.setDecorFitsSystemWindows(requireActivity().window, true)
                WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
                    controller.isAppearanceLightStatusBars = false
                }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                /*
                return when (item?.itemId) {
                     R.id.share -> {
                         // Handle share icon press
                         true
                     }
                     R.id.delete -> {
                         // Handle delete icon press
                         true
                     }
                     R.id.more -> {
                         // Handle more item (inside overflow menu) press
                         true
                     }
                     else -> false
                 }
                 */
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                tracker.clearSelection()
                WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
                    controller.isAppearanceLightStatusBars = true
                }
                //  WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
            }
        }
        tracker.addObserver(object: SelectionTracker.SelectionObserver<Long>() {
            var actionMode: ActionMode? = null

            override fun onSelectionChanged() {
                super.onSelectionChanged()

                actionMode?.title = tracker.selection.size().toString()
                if (actionMode == null) {
                    actionMode = (parentFragment as BottomNavFrag).startActionMode(callback)
                    //    val inflater = requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    //   val actionBinding = ActionModeToolbarBinding.inflate(inflater)
                    //  actionMode?.customView = actionBinding.root
                    //actionMode = activity?.startActionMode(callback)
                } else if (tracker.selection.size() == 0) {
                    actionMode?.finish()
                    actionMode = null
                }
            }

            override fun onSelectionCleared() {
                actionMode?.finish()
                actionMode = null
            }
        })

        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()
        return binding.root
    }


    private fun scrollToPosition() {
        binding.root.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
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
                val viewAtPosition = if (MainActivity.viewPagerScrollDirectionDownwards) {
                    binding.rvItems.layoutManager!!.findViewByPosition(MainActivity.currentListPosition + spanCount)
                } else {
                    binding.rvItems.layoutManager!!.findViewByPosition(MainActivity.currentListPosition)
                }
                // Scroll to position if the view for the current position is null (not currently part of
                // layout manager children), or it's not completely visible.
                if (viewAtPosition == null || binding.rvItems.layoutManager!!
                        .isViewPartiallyVisible(viewAtPosition, true, true)
                ) {
                    if (MainActivity.viewPagerScrollDirectionDownwards) {
                        binding.rvItems.post { binding.rvItems.layoutManager!!.scrollToPosition(MainActivity.currentListPosition + spanCount) }
                    } else {
                        binding.rvItems.post { binding.rvItems.layoutManager!!.scrollToPosition(MainActivity.currentListPosition) }
                    }

                }
            }
        })
    }

    companion object {
        var spanCount = 4
    }

}