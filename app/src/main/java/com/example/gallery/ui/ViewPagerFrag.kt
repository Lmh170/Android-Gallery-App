package com.example.gallery.ui

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.gallery.ListItem
import com.example.gallery.R
import com.example.gallery.adapter.ViewPagerAdapter
import com.example.gallery.databinding.FragmentViewPagerBinding
import com.example.gallery.databinding.ViewDialogInfoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import java.text.SimpleDateFormat
import java.util.*

class ViewPagerFrag : Fragment() {
    private lateinit var _binding: FragmentViewPagerBinding
    val binding get() = _binding
    private val viewModel: MainViewModel by activityViewModels()
    private var isSystemUiVisible = true
    private var shortAnimationDuration = 0L
    private var firstCurrentItem = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::_binding.isInitialized){
            _binding = FragmentViewPagerBinding.inflate(inflater, container, false)
        }
        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        binding.tbViewPager.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setUpViewpager()
        prepareSharedElementTransition()
        setUpViews()
        return binding.root
    }

    private fun setUpViewpager() {
        val adapter = ViewPagerAdapter(this)

        if (requireArguments().getBoolean("isAlbum")) {
            adapter.submitList(viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems?.value)
       //     adapter.submitList(viewModel.albums.value?.get(MainActivity.currentAlbumPosition)?.mediaItems?.value)
            viewModel.albums.observe(viewLifecycleOwner, {albums ->
                val items = albums.find { it.name == MainActivity.currentAlbumName }?.mediaItems?.value
                (binding.viewPager.adapter as ViewPagerAdapter).submitList(items)
            })
     //       viewModel.albums.value?.get(MainActivity.currentAlbumPosition)?.mediaItems?.observe(viewLifecycleOwner, { items ->
         //       (binding.viewPager.adapter as ViewPagerAdapter).submitList(items)
        //    })
        } else {
            adapter.submitList(viewModel.viewPagerImages.value)
            viewModel.viewPagerImages.observe(viewLifecycleOwner, { items ->
                (binding.viewPager.adapter as ViewPagerAdapter).submitList(items)
            })
        }

        binding.viewPager.apply {
            this.adapter = adapter
            firstCurrentItem = MainActivity.currentViewPagerPosition
            setCurrentItem(MainActivity.currentViewPagerPosition, false)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    MainActivity.currentViewPagerPosition = position
                    if (requireArguments().getBoolean("isAlbum")) {
                        MainActivity.currentListPosition = position
                    } else {
                        MainActivity.currentListPosition = viewModel.viewPagerImages.value?.get(position)!!.listPosition
                    }
                }
            })
            setPageTransformer(MarginPageTransformer(50))
        }
    }

    fun showSystemUI() {
        binding.tbViewPager.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimationDuration
        }

        binding.cvEdit.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimationDuration
        }
        binding.cvShare.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimationDuration
        }
        binding.cvInfo.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimationDuration
        }
        binding.cvDelete.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimationDuration
        }
        binding.ivGradTop.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimationDuration
        }
        binding.ivGardBottom.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimationDuration
        }
        WindowInsetsControllerCompat(requireActivity().window, requireActivity().window.decorView).show(WindowInsetsCompat.Type.systemBars())
        /*
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

         */
    }

    fun hideSystemUI() {
        binding.tbViewPager.apply {
            animate().alpha(0f).setDuration(shortAnimationDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })

        }
        binding.cvShare.apply {
            animate().alpha(0f).setDuration(shortAnimationDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }
        binding.cvEdit.apply {
            animate().alpha(0f).setDuration(shortAnimationDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }
        binding.cvInfo.apply {
            animate().alpha(0f).setDuration(shortAnimationDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
            //   animate().alpha(0f).duration = shortAnimationDuration

        }
        binding.cvDelete.apply {
            animate().alpha(0f).setDuration(shortAnimationDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }
        binding.ivGradTop.apply {
            animate().alpha(0f).setDuration(shortAnimationDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }
        binding.ivGardBottom.apply {
            animate().alpha(0f).setDuration(shortAnimationDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }

        WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        }
    }

    fun toggleSystemUI() {
        if (isSystemUiVisible) hideSystemUI() else showSystemUI()
        isSystemUiVisible = !isSystemUiVisible
    }

    private fun setUpSystemBars() {
        WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    private fun setUpViews() {
        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.tbViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams>{
                topMargin = insets.top
            }
            binding.cvShare.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            binding.cvEdit.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            binding.cvInfo.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            binding.cvDelete.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            return@setOnApplyWindowInsetsListener windowInsets
        }
        binding.cvShare.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            val share = Intent(Intent.ACTION_SEND)
            share.type = activity?.contentResolver?.getType(currentItem.uri)
            share.putExtra(Intent.EXTRA_STREAM, currentItem.uri)
            startActivity(Intent.createChooser(share, "Share with"))
        }
        binding.cvDelete.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialAlertDialog_Centered)
                    .setTitle("Permanently delete?")
                    .setMessage("This image will be permanently deleted.")
                    .setIcon(R.drawable.ic_outline_delete_24)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteImage(getCurrentItem())
                    }
                    .show()

            } else viewModel.deleteImage(getCurrentItem())
        }
        binding.cvEdit.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            val editIntent = Intent(Intent.ACTION_EDIT)
            editIntent.type = activity?.contentResolver?.getType(currentItem.uri)
            editIntent.data = currentItem.uri
            editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            //share.putExtra(Intent.EXTRA_STREAM, (getCurrentItem()?.uri))
            startActivity(Intent.createChooser(editIntent, "Edit with"))
        }
        binding.cvInfo.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            val inflater = requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val binding = ViewDialogInfoBinding.inflate(inflater)
            binding.tvDateAdded.text = SimpleDateFormat.getDateInstance().format(Date(currentItem.dateAdded))
            binding.tvName.text = currentItem.name
            binding.tvTimeAdded.text = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(currentItem.dateAdded))
            binding.tvPath.text = currentItem.path
            binding.tvSize.text = currentItem.size.div(1000000).toString() + " MB"

            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialAlertDialog_Centered)
                .setTitle("Info")
                .setView(binding.root)
                .setIcon(R.drawable.ic_outline_info_24)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    private fun getCurrentItem():ListItem.MediaItem?  {
        return if (requireArguments().getBoolean("isAlbum")) {
            viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems?.value?.get(binding.viewPager.currentItem)
        } else {
            viewModel.viewPagerImages.value?.get(binding.viewPager.currentItem)
        }
    }

    override fun startPostponedEnterTransition() {
        super.startPostponedEnterTransition()
        setUpSystemBars()
    }

    private fun prepareSharedElementTransition() {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            scrimColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                resources.getColor(android.R.color.black, requireActivity().theme)
            } else {
                resources.getColor(android.R.color.black)
            }
        }
        // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {
                    // Locate the image view at the primary fragment (the ImageFragment that is currently
                    // visible). To locate the fragment, call instantiateItem with the selection position.
                    // At this stage, the method will simply return the fragment at the position and will
                    // not create a new one.
                    /*
                    val currentFragment = viewPager.adapter
                        ?.instantiateItem(viewPager, MainActivity.currentPosition) as Fragment
                    val view = currentFragment.view ?: return

                     */

                    val selectedViewHolder =
                        (binding.viewPager.getChildAt(0) as RecyclerView?)?.findViewHolderForAdapterPosition(binding.viewPager.currentItem)
                            as ViewPagerAdapter.ViewHolder? ?: return

               //     selectedViewHolder.bindTransitionImage()
                  //  selectedViewHolder.binding.transitionImage.visibility = View.VISIBLE

                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] = selectedViewHolder.binding.pagerImage
                    // sharedElements[names[0]] = view.findViewById(R.id.image) as ImageView


                }
            })
        postponeEnterTransition()
    }

}