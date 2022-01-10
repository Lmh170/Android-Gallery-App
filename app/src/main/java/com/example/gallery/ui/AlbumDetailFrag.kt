package com.example.gallery.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentAlbumDetailBinding
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import android.view.ViewGroup
import com.example.gallery.ListItem


class AlbumDetailFrag : Fragment() {
    private lateinit var _binding: FragmentAlbumDetailBinding
    private val binding get() = _binding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (::_binding.isInitialized) return binding.root
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)

        val adapter = GridItemAdapter(this@AlbumDetailFrag, true)
        adapter.setHasStableIds(true)
        binding.rvAlbums.apply{
            setHasFixedSize(true)
            this.adapter = adapter
        }
        BottomNavFrag.enteringFromAlbum = true

        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.tbAlbum.updateLayoutParams<ViewGroup.MarginLayoutParams>{
                topMargin = insets.top
            }
            return@setOnApplyWindowInsetsListener windowInsets
        }

        viewModel.albums.observe(viewLifecycleOwner, { albums->
            val items = albums.find { it.name == MainActivity.currentAlbumName }?.mediaItems
            val position = (binding.rvAlbums.layoutManager as GridLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
            adapter.submitList(items as List<ListItem>) {
                if (position == 0) binding.rvAlbums.scrollToPosition(0)
            }
        })

        binding.tbAlbum.title = MainActivity.currentAlbumName

        binding.tbAlbum.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        prepareTransitions()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        setUpSystemBars()
        viewModel.albums.observe(viewLifecycleOwner, { albums->
            val items = albums.find { it.name == MainActivity.currentAlbumName }?.mediaItems
            val position = (binding.rvAlbums.layoutManager as GridLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
            (binding.rvAlbums.adapter as GridItemAdapter).submitList(items as List<ListItem>) {
                if (position == 0) binding.rvAlbums.scrollToPosition(0)
            }
        })

      //  (binding.rvAlbums.adapter as GridItemAdapter).submitList(
      //      viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems?.value
     //   ) {

            scrollToPosition()
            /*
            val firstPosition = (binding.rvAlbums.layoutManager as GridLayoutManager)
                .findFirstVisibleItemPosition()
            val lastPosition = (binding.rvAlbums.layoutManager as GridLayoutManager)
                .findLastVisibleItemPosition()
            val viewAtPosition =
                binding.rvAlbums.layoutManager!!.findViewByPosition(MainActivity.currentListPosition)
            println("first $firstPosition last $lastPosition")
            if (viewAtPosition != null || !(MainActivity.currentListPosition < firstPosition || MainActivity.currentListPosition > lastPosition)) {
                //   postponeEnterTransition()
                scrollToPosition()

            } else {
                scrollToPosition()
                // postponeEnterTransition()
                println("in if")
            }
//        scrollToPosition()
            setUpSystemBars()
            //    prepareTransitions()
            */
    //    }



    }

    private fun scrollToPosition() {
        binding.rvAlbums.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
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
                binding.rvAlbums.removeOnLayoutChangeListener(this)

                val viewAtPosition =
                    binding.rvAlbums.layoutManager!!.findViewByPosition(MainActivity.currentListPosition)

                // Scroll to position if the view for the current position is null (not currently part of
                // layout manager children), or it's not completely visible.
                if (viewAtPosition == null || !binding.rvAlbums.layoutManager!!
                        .isViewPartiallyVisible(viewAtPosition, true, true)
                ) {
                    binding.rvAlbums.post {
                        binding.rvAlbums.layoutManager!!.scrollToPosition(MainActivity.currentListPosition)
                        startPostponedEnterTransition()
                    }
                } else {
                    startPostponedEnterTransition()
                }
            }
        })
    }

    private fun prepareTransitions() {
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z,true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z,false)
        exitTransition = Hold()
        setExitSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {

                    // Locate the ViewHolder for the clicked position.
                    val selectedViewHolder = binding.rvAlbums
                        .findViewHolderForAdapterPosition(MainActivity.currentListPosition) ?: return
//                    (exitTransition as Hold).excludeChildren((selectedViewHolder as GridAdapter.MediaItemHolder).binding.image, true)

                    // Map the first shared element name to the child ImageView.

                    sharedElements[names[0]] =
                        (selectedViewHolder as GridItemAdapter.MediaItemHolder).binding.image

                }
            })
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
}