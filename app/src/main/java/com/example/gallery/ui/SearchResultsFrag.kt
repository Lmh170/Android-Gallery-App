package com.example.gallery.ui

import android.app.SearchManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.R
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentSearchResultsBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.shape.MaterialShapeDrawable

class SearchResultsFrag : Fragment() {
    private lateinit var _binding: FragmentSearchResultsBinding
    private val binding: FragmentSearchResultsBinding get() = _binding

    private val viewModel: MainViewModel by activityViewModels()
    private var actionMode: ActionMode? = null
    private lateinit var tracker: SelectionTracker<Long>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (::_binding.isInitialized) return binding.root

        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)

        viewModel.viewPagerItems.observe(viewLifecycleOwner) {
            (binding.rvSearchResults.adapter as GridItemAdapter).submitList(it)

            binding.tbSearch.searchInput.setQuery(
                requireActivity().intent.getStringExtra(SearchManager.QUERY),
                false
            )
        }
        binding.tbSearch.toolbar.apply {
            setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
            setNavigationOnClickListener {
                requireActivity().finish()
            }
            updatePadding(
                left = 0
            )
        }
        binding.tbSearch.appBarLayout.apply {
            fitsSystemWindows = true
            statusBarForeground = MaterialShapeDrawable
                .createWithElevationOverlay(binding.tbSearch.appBarLayout.context)
        }

        binding.tbSearch.searchInput.apply {
            val searchManager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
            setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))
            binding.tbSearch.searchInput.setQuery(
                requireActivity().intent.getStringExtra(SearchManager.QUERY),
                false
            )
        }

        binding.tbSearch.btnSearchDate.setOnClickListener {
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select dates")
                .build()
                .also { picker ->
                    picker.show(childFragmentManager, it.toString())
                    picker.addOnPositiveButtonClickListener { pair ->
                        binding.tbSearch.searchInput.setQuery(
                            "DATE:${pair.first.div(1000)}/${pair.second.div(1000)}",
                            true
                        )
                    }
                }
        }


        binding.rvSearchResults.apply {
            isVisible = true
            adapter = GridItemAdapter(this@SearchResultsFrag, false) { extras, _ ->
                findNavController().navigate(
                    R.id.action_searchResultsFrag_to_viewPagerFrag2,
                    null,
                    null,
                    extras
                )
            }.apply {
                submitList(viewModel.viewPagerItems.value)
            }

            layoutManager =
                GridLayoutManager(requireContext(), resources.getInteger(R.integer.spanCount))
            setHasFixedSize(true)
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        setUpSystemBars()
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