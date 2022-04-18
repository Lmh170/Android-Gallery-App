package com.example.gallery.ui

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.gallery.R
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentBottomNavBinding
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis

class BottomNavFrag : Fragment() {
    private lateinit var _binding: FragmentBottomNavBinding
    private val binding get() = _binding
    private lateinit var actionMode: ActionMode

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

        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.tbMain.updateLayoutParams<ViewGroup.MarginLayoutParams>{
                topMargin = insets.top
            }
            binding.bnvMain.updatePadding(0, 0, 0, insets.bottom)
            return@setOnApplyWindowInsetsListener windowInsets
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
                    if (::actionMode.isInitialized) actionMode.finish()
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
        actionMode =  binding.tbMain.startActionMode(callback)
        return actionMode
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

    private fun setSystemBarsAppearance() {

        val nightModeFlags: Int = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO ||
                nightModeFlags == Configuration.UI_MODE_NIGHT_UNDEFINED){
                    activity?.window?.insetsController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                activity?.window?.insetsController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)

            }
        } else {
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO ||
                nightModeFlags == Configuration.UI_MODE_NIGHT_UNDEFINED){
                    activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
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
                    var frag: GridItemFrag?
                    try {
                        frag = childFragmentManager.findFragmentById(R.id.fcvBottomNav) as GridItemFrag?
                    } catch (e: ClassCastException) {
                        throw e
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