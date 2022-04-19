package com.example.gallery.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.gallery.ListItem
import com.example.gallery.R
import com.example.gallery.adapter.ViewPagerAdapter
import com.example.gallery.databinding.FragmentViewPagerBinding
import com.example.gallery.databinding.ViewDialogInfoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFade
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.set


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
        when {
            requireArguments().getBoolean("isAlbum") -> {
                viewModel.albums.observe(viewLifecycleOwner) { albums ->
                    val items = albums.find { it.name == MainActivity.currentAlbumName }?.mediaItems
                    (binding.viewPager.adapter as ViewPagerAdapter).submitList(items)
                }
            }
            else -> {
                viewModel.viewPagerImages.observe(viewLifecycleOwner) { items ->
                    (binding.viewPager.adapter as ViewPagerAdapter).submitList(items)
                }
            }
        }
        prepareSharedElementTransition()
        if (::_binding.isInitialized) {
            return binding.root
        }

        _binding = FragmentViewPagerBinding.inflate(inflater, container, false)

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
            .toLong()
        binding.tbViewPager.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.loadItems()
        setUpViewpager()
        setUpViews()
        return binding.root
    }

    private fun setUpViewpager() {
        val adapter = ViewPagerAdapter(this)

        binding.viewPager.apply {
            this.adapter = adapter
            if (requireArguments().getBoolean("isAlbum")) {
                adapter.submitList(viewModel.albums.value?.find {
                    it.name == MainActivity.currentAlbumName
                }?.mediaItems)
            } else {
                adapter.submitList(viewModel.viewPagerImages.value)
            }

            firstCurrentItem = MainActivity.currentViewPagerPosition
            setCurrentItem(MainActivity.currentViewPagerPosition, false)

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    MainActivity.currentViewPagerPosition = position
                    if (requireArguments().getBoolean("isAlbum")) {
                        MainActivity.currentListPosition = position
                    } else {
                        MainActivity.currentListPosition =
                            viewModel.viewPagerImages.value?.get(position)!!.listPosition
                    }
                }
            })
            setPageTransformer(MarginPageTransformer(50))
        }
    }

    fun showSystemUI() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 250L
            excludeTarget(binding.ivGradTop, true)
            excludeTarget(binding.ivGardBottom, true)
        })

        binding.cvInfo.visibility = View.VISIBLE
        binding.cvDelete.visibility = View.VISIBLE

        binding.cvEdit.visibility = View.VISIBLE
        binding.tbViewPager.visibility = View.VISIBLE
        binding.cvShare.visibility = View.VISIBLE
        binding.ivGradTop.visibility = View.VISIBLE
        binding.ivGardBottom.visibility = View.VISIBLE

        ViewCompat.getWindowInsetsController(requireActivity().window.decorView)
            ?.show(WindowInsetsCompat.Type.systemBars())
    }

    fun hideSystemUI() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 180L
            excludeTarget(binding.ivGradTop, true)
            excludeTarget(binding.ivGardBottom, true)
        })

        binding.tbViewPager.visibility = View.GONE
        binding.cvShare.visibility = View.GONE
        binding.cvEdit.visibility = View.GONE
        binding.cvInfo.visibility = View.GONE
        binding.cvDelete.visibility = View.GONE
        binding.ivGradTop.visibility = View.GONE
        binding.ivGardBottom.visibility = View.GONE

        //   windowInsetsController.systemBarsBehavior =
        //    WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH

        ViewCompat.getWindowInsetsController(requireActivity().window.decorView)
            ?.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun toggleSystemUI() {
        if (isSystemUiVisible) hideSystemUI() else showSystemUI()
        isSystemUiVisible = !isSystemUiVisible
    }

    private fun setUpSystemBars() {
        try {
            WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        } catch (e: IllegalStateException) {

        }
    }

    private fun setUpViews() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { _, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.tbViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = insets.top
                }
                binding.cvShare.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }
                binding.cvEdit.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }
                binding.cvInfo.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }
                binding.cvDelete.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }
                return@setOnApplyWindowInsetsListener windowInsets
            }
        }

        binding.cvShare.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            share(currentItem, requireActivity())
        }
        binding.cvDelete.setOnClickListener {
            getCurrentItem()?.let { delete(it, requireContext(), viewModel) }
        }
        binding.cvEdit.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            val editIntent = Intent(Intent.ACTION_EDIT)
            editIntent.type = activity?.contentResolver?.getType(currentItem.uri)
            editIntent.data = currentItem.uri
            editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(editIntent, "Edit with"))
        }
        binding.cvInfo.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            val info = viewModel.getImageInfo(currentItem.uri)
            val inflater = requireActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val binding = ViewDialogInfoBinding.inflate(inflater)
            binding.tvDateAdded.text = SimpleDateFormat.getDateInstance().format(
                Date(
                    info[0]
                        .toLong()
                )
            )
            binding.tvName.text = info[3]
            binding.tvTimeAdded.text = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
                .format(Date(info[0].toLong()))
            binding.tvPath.text = info[2]
            binding.tvSize.text = String.format(resources.getString(R.string.item_size), info[1])

            MaterialAlertDialogBuilder(
                requireContext(), R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle(resources.getString(R.string.info))
                .setView(binding.root)
                .setIcon(R.drawable.ic_outline_info_24)
                .setPositiveButton(getString(R.string.close), null)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WindowInsetsControllerCompat(requireActivity().window, requireActivity().window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        fun delete(image: ListItem.MediaItem, context: Context, viewModel: MainViewModel) {
            MaterialAlertDialogBuilder(
                context,
                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle("Permanently delete?")
                .setMessage("This item will be permanently deleted.")
                .setIcon(R.drawable.ic_outline_delete_24)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteImage(image)
                }
                .show()
        }

        fun delete(images: List<ListItem.MediaItem>, context: Context, viewModel: MainViewModel) {
            MaterialAlertDialogBuilder(
                context,
                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle("Permanently delete?")
                .setMessage("This items will be permanently deleted.")
                .setIcon(R.drawable.ic_outline_delete_24)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteImages(images)
                }
                .show()
        }

        fun share(item: ListItem.MediaItem, activity: Activity) {
            val share = Intent(Intent.ACTION_SEND)
            share.data = item.uri
            share.type = activity.contentResolver.getType(item.uri)
            share.putExtra(Intent.EXTRA_STREAM, item.uri)
            share.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            activity.startActivity(Intent.createChooser(share, "Share with"))
        }

        fun share(items: List<ListItem.MediaItem>, activity: Activity) {
            val share = Intent(Intent.ACTION_SEND_MULTIPLE)
            val uris = ArrayList<Uri>()
            for (item in items) {
                uris.add(item.uri)
            }
            share.type = "*/*"
            share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            share.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            activity.startActivity(Intent.createChooser(share, "Share with"))
        }
    }

    private fun getCurrentItem(): ListItem.MediaItem? {
        return try {
            (binding.viewPager.adapter as ViewPagerAdapter).currentList[binding.viewPager.currentItem]
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    override fun startPostponedEnterTransition() {
        super.startPostponedEnterTransition()
        setUpSystemBars()
    }

    private fun prepareSharedElementTransition() {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            scrimColor =
                resources.getColor(android.R.color.black, requireActivity().theme)
        }

        setEnterSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {
                    val selectedViewHolder =
                        (binding.viewPager.getChildAt(0) as RecyclerView?)
                            ?.findViewHolderForLayoutPosition(binding.viewPager.currentItem)
                                as ViewPagerAdapter.ViewHolderPager? ?: return

                    sharedElements[names[0]] = selectedViewHolder.binding.pagerImage
                }
            })
        postponeEnterTransition()
    }
}