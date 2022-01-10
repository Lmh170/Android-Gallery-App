package com.example.gallery.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
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
        scrollToPosition()
        ViewGroupCompat.setTransitionGroup(binding.rvItems, true)
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
                val viewAtPosition =
                    binding.rvItems.layoutManager!!.findViewByPosition(MainActivity.currentListPosition)

                // Scroll to position if the view for the current position is null (not currently part of
                // layout manager children), or it's not completely visible.
                if (viewAtPosition == null || !binding.rvItems.layoutManager!!
                        .isViewPartiallyVisible(viewAtPosition, true, true)
                ) {
                    binding.rvItems.post { binding.rvItems.layoutManager!!.scrollToPosition(MainActivity.currentListPosition) }
                }
            }
        })
    }

    companion object {
        var spanCount = 4
    }

}