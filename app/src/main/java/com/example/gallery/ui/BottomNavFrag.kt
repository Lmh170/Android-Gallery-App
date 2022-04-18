package com.example.gallery.ui

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.gallery.R
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentBottomNavBinding
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis

class BottomNavFrag : Fragment() {
    private lateinit var _binding: FragmentBottomNavBinding
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::_binding.isInitialized){
            _binding = FragmentBottomNavBinding.inflate(inflater, container, false)
        }
        val frag = childFragmentManager.findFragmentById(R.id.fcvBottomNav)
        if (frag is GridItemFrag){
            postponeEnterTransition()
            prepareTransitions()
        }

        binding.appBarLayout.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(requireContext())

        binding.fcvBottomNav.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = binding.bnvMain.measuredHeight
        }

        binding.bnvMain.setOnItemReselectedListener {
            val frag = childFragmentManager.findFragmentById(R.id.fcvBottomNav)
            if (frag is GridItemFrag) {
                frag.binding.rvItems.smoothScrollToPosition(0)
            } else if (frag is GridAlbumFrag) {
                frag.binding.rvAlbum.smoothScrollToPosition(0)
            }
        }
        binding.bnvMain.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.item_photos -> {
                    childFragmentManager.commit {
                        replace<GridItemFrag>(R.id.fcvBottomNav)
                        setReorderingAllowed(true)
                        MainActivity.currentListPosition = 0
                    }
                    true
                }
                R.id.item_albus -> {
                    if (frag is GridItemFrag && frag.actionMode != null) {
                        frag.actionMode?.finish()
                        frag.actionMode = null
                        activity?.window?.statusBarColor = resources.getColor(R.color.material_dynamic_primary95, activity?.theme)
                    }
                    childFragmentManager.commit {
                        replace<GridAlbumFrag>(R.id.fcvBottomNav)
                        setReorderingAllowed(true)
                    }
                    true
                }
                else -> false
            }
        }
        return binding.root
    }

    fun startActionMode(callback: ActionMode.Callback): ActionMode {
        activity?.window?.statusBarColor = resources.getColor(R.color.material_dynamic_primary95, activity?.theme)
        return binding.tbMain.startActionMode(callback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareTransitions()
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

    override fun onStart() {
        super.onStart()
        setUpSystemBars()
    }

    fun prepareTransitions() {

    //     exitTransition = TransitionInflater.from(context)
      //    .inflateTransition(R.transition.grid_exit_transition)
        exitTransition = if (enteringFromAlbum) {
            MaterialSharedAxis(MaterialSharedAxis.Z, false)
        } else {
            Hold()
        }
        //     reenterTransition = MaterialElevationScale(true)
        //  exitTransition = Hold()
        // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
        setExitSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {
                    val frag: GridItemFrag?
                    try {
                        frag = childFragmentManager.findFragmentById(R.id.fcvBottomNav) as GridItemFrag?
                    } catch (e: ClassCastException) {
                        return
                    }
                    if (frag != null) {
                        ViewGroupCompat.setTransitionGroup(frag.binding.rvItems, false)
                    }
                    // Locate the ViewHolder for the clicked position.
                    val selectedViewHolder = frag?.binding?.rvItems
                        ?.findViewHolderForAdapterPosition(MainActivity.currentListPosition) ?: return

//                    (exitTransition as Hold).excludeChildren((selectedViewHolder as GridAdapter.MediaItemHolder).binding.image, true)


                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] =
                        (selectedViewHolder as GridItemAdapter.MediaItemHolder).binding.image

                }
            })
    }

    companion object {
         var enteringFromAlbum = false
    }

}