package com.example.gallery.ui

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.R
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentBottomNavBinding
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis

class BottomNavFrag : Fragment() {
    private lateinit var _binding: FragmentBottomNavBinding
    val binding get() = _binding
    var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (::_binding.isInitialized){
            val frag = childFragmentManager.findFragmentById(R.id.fcvBottomNav)
            if (frag is GridItemFrag && MainActivity.currentListPosition != Int.MIN_VALUE){
                postponeEnterTransition()
                prepareTransitions()
            }
            return binding.root
        }
        _binding = FragmentBottomNavBinding.inflate(inflater, container, false)

        if (requireActivity().intent.action == Intent.ACTION_PICK || requireActivity().intent.action ==
                Intent.ACTION_GET_CONTENT) {
            binding.tbMain.isTitleCentered = false
            binding.tbMain.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.tbMain.setNavigationIconTint(resources.getColor(android.R.color.black, activity?.theme))
            } else {
                binding.tbMain.setNavigationIconTint(resources.getColor(android.R.color.black))
            }
            if (!requireActivity().intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                    false)) {
                binding.tbMain.title = "Select an item"
            } else {
                binding.tbMain.title = "Select items"
            }
            binding.bnvMain.visibility = View.GONE
            childFragmentManager.commit {
                replace<GridAlbumFrag>(R.id.fcvBottomNav)
                setReorderingAllowed(true)
            }
            binding.tbMain.setNavigationOnClickListener {
                requireActivity().setResult(Activity.RESULT_CANCELED)
                requireActivity().finish()
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                binding.tbMain.inflateMenu(R.menu.action_bar_home)
            }
            binding.tbMain.setOnMenuItemClickListener {
                when(it.itemId) {
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
        binding.bnvMain.viewTreeObserver.addOnGlobalLayoutListener {
            binding.fcvBottomNav.updatePadding(0, 0, 0, binding.bnvMain.height)
        }

        binding.appBarLayout.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(requireContext())

        binding.bnvMain.setOnItemReselectedListener {
            val fragment = childFragmentManager.findFragmentById(R.id.fcvBottomNav)
            if (fragment is GridItemFrag) {
                fragment.binding.rvItems.scrollToPosition(0)
                binding.appBarLayout.setExpanded(true)
            } else if (fragment is GridAlbumFrag) {
                fragment.binding.rvAlbum.scrollToPosition(0)
                binding.appBarLayout.setExpanded(true)
            }
        }
        binding.bnvMain.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.miPhotos -> {
                    childFragmentManager.commit {
                        replace<GridItemFrag>(R.id.fcvBottomNav)
                        setReorderingAllowed(true)
                        MainActivity.currentListPosition = 0
                    }.also {
                        binding.appBarLayout.setExpanded(true)
                    }
                    true
                }
                R.id.miAlbums -> {
                    actionMode?.finish()
                    actionMode = null
                    childFragmentManager.commit {
                        replace<GridAlbumFrag>(R.id.fcvBottomNav)
                        setReorderingAllowed(true)
                    }.also {
                        binding.appBarLayout.setExpanded(true)
                    }
                    true
                }
                else -> false
            }
        }
        prepareTransitions()
        return binding.root
    }

    private fun setUpSystemBars() {
        val nightModeFlags: Int = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO ||
            nightModeFlags == Configuration.UI_MODE_NIGHT_UNDEFINED) {
            WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    fun startActionMode(callback: ActionMode.Callback): ActionMode {
        activity?.window?.statusBarColor =  SurfaceColors.getColorForElevation(requireContext(), binding.appBarLayout.elevation) //resources.getColor(R.color.material_dynamic_neutral_variant20, activity?.theme)
        actionMode = binding.tbMain.startActionMode(callback)
        return actionMode!!
    }

    override fun onStart() {
        super.onStart()
        setUpSystemBars()
    }

    fun setSharedAxisTransition () {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z,true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z,false)
    }

    fun setHoldTransition() {
        exitTransition = Hold()
        reenterTransition = Hold()
    }

    fun prepareTransitions() {
        setExitSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {
                    val frag: GridItemFrag = childFragmentManager.findFragmentById(R.id.fcvBottomNav) as GridItemFrag
                    if ((frag.binding.rvItems.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition() != 0) {
                        binding.appBarLayout.setExpanded(false, false)
                    }

                    frag.binding.rvItems.isTransitionGroup = false

                    // Locate the ViewHolder for the clicked position.
                    val selectedViewHolder = frag.binding.rvItems
                        .findViewHolderForLayoutPosition(MainActivity.currentListPosition) ?: return

                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] =
                        (selectedViewHolder as GridItemAdapter.MediaItemHolder).binding.image

                }
            }
        )
    }
}