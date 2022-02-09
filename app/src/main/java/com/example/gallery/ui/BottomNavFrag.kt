package com.example.gallery.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.findFragment
import androidx.fragment.app.replace
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.R
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentBottomNavBinding
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import java.util.concurrent.atomic.AtomicBoolean

class BottomNavFrag : Fragment() {
    private lateinit var _binding: FragmentBottomNavBinding
    val binding get() = _binding
    var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (requireActivity().intent.action == Intent.ACTION_VIEW) {
            val args = Bundle()
            args.putParcelable("item", requireActivity().intent.data)
            findNavController()
                .navigate(
                    R.id.action_bottomNavFrag_to_viewPagerFrag,
                    args
                )
        }
        if (!::_binding.isInitialized){
            _binding = FragmentBottomNavBinding.inflate(inflater, container, false)
        }
        val frag = childFragmentManager.findFragmentById(R.id.fcvBottomNav)
        if (frag is GridItemFrag){
            postponeEnterTransition()
            prepareTransitions()
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
                R.id.item_photos -> {
                    binding.appBarLayout.setExpanded(true)
                    childFragmentManager.commit {
                        replace<GridItemFrag>(R.id.fcvBottomNav)
                        setReorderingAllowed(true)
                        MainActivity.currentListPosition = 0
                    }
                    true
                }
                R.id.item_albus -> {
                    actionMode?.finish()
                    actionMode = null
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

    fun startActionMode(callback: ActionMode.Callback): ActionMode {
        activity?.window?.statusBarColor = resources.getColor(R.color.material_dynamic_primary95, activity?.theme)
        actionMode = binding.tbMain.startActionMode(callback)
        return actionMode!!
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

                    val frag: GridItemFrag = childFragmentManager.findFragmentById(R.id.fcvBottomNav) as GridItemFrag
                    if ((frag.binding.rvItems.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition() != 0) {
                        binding.appBarLayout.setExpanded(false, false)
                    }

                    ViewGroupCompat.setTransitionGroup(frag.binding.rvItems, false)

                    // Locate the ViewHolder for the clicked position.
                    val selectedViewHolder = frag.binding.rvItems
                        .findViewHolderForAdapterPosition(MainActivity.currentListPosition) ?: return

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