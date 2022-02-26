package com.example.gallery.ui

import android.annotation.SuppressLint
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
import java.net.URLDecoder
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
        if (::_binding.isInitialized){
            return binding.root
        }
        
        _binding = FragmentViewPagerBinding.inflate(inflater, container, false)

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
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
                adapter.submitList(viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems)
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
                        MainActivity.currentListPosition = viewModel.viewPagerImages.value?.get(position)!!.listPosition
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
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(requireActivity().window.decorView) ?: return
        // Hide both the status bar and the navigation bar
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
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

        val windowInsetsController =
                ViewCompat.getWindowInsetsController(requireActivity().window.decorView) ?: return
            // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
            // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
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
            windowInsets
        }
        binding.cvShare.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            share(currentItem, requireActivity())
        }
        binding.cvDelete.setOnClickListener {
            getCurrentItem()?.let {delete(it, requireContext(), viewModel) }
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
            val inflater = requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val binding = ViewDialogInfoBinding.inflate(inflater)
            binding.tvDateAdded.text = SimpleDateFormat.getDateInstance().format(Date(info[0].toLong()))
            binding.tvName.text = info[3]
            binding.tvTimeAdded.text = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(Date(info[0].toLong()))
            binding.tvPath.text = info[2]
            binding.tvSize.text = info[1] + " MB"

            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialAlertDialog_Centered)
                .setTitle("Info")
                .setView(binding.root)
                .setIcon(R.drawable.ic_outline_info_24)
                .setPositiveButton("Close", null)
                .show()


            // showCurrentMediaDetails()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WindowInsetsControllerCompat(requireActivity().window, requireActivity().window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        fun delete(image: ListItem.MediaItem, context: Context, viewModel: MainViewModel) {
            MaterialAlertDialogBuilder(context, R.style.Theme_MaterialAlertDialog_Centered)
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
            MaterialAlertDialogBuilder(context, R.style.Theme_MaterialAlertDialog_Centered)
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
            for (item in items){
                uris.add(item.uri)
            }
            share.type = "*/*"
            share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            share.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            activity.startActivity(Intent.createChooser(share, "Share with"))
        }
    }
/*
    private fun showCurrentMediaDetails() {
        val item = getCurrentItem() ?:return

        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE
            )
        } else {
            arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE
            )
        }

        val mediaCursor = viewModel.getApplication<Application>().contentResolver.query(
            item.uri,
            projection,
            null,
            null,
        )

        if (mediaCursor?.moveToFirst() != true) {
            Toast.makeText(context, "An unexpected error occurred", Toast.LENGTH_SHORT).show()

            mediaCursor?.close()
            return
        }

       val relativePath = mediaCursor.getString(0)
      val fileName = mediaCursor.getString(1)
       val size = mediaCursor.getInt(2)

        mediaCursor.close()

        var infos = viewModel.getImageInfo(item)


        var dateAdded : String? = null
        var dateModified : String? = null

        if (item.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {

            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(
                context,
                item.uri
            )

            val date =
                convertTimeForVideo(
                    mediaMetadataRetriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DATE
                    )!!
                )

            dateAdded = date
            dateModified = date

        } else {
            val iStream = requireActivity().contentResolver.openInputStream(
                item.uri
            )
            val eInterface = ExifInterface(iStream!!)

            val offset = eInterface.getAttribute(ExifInterface.TAG_OFFSET_TIME)

            if (eInterface.hasAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)) {
                dateAdded = convertTimeForPhoto(
                    eInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)!!,
                    offset
                )
            }

            if (eInterface.hasAttribute(ExifInterface.TAG_DATETIME)) {
                dateModified = convertTimeForPhoto(
                    eInterface.getAttribute(ExifInterface.TAG_DATETIME)!!,
                    offset
                )
            }

            iStream.close()
        }



        val alertDialog = MaterialAlertDialogBuilder(requireContext())
        alertDialog.setIcon(R.drawable.ic_outline_info_24)
        alertDialog.setTitle("File Details")

        val detailsBuilder = StringBuilder()

        detailsBuilder.append("\nFile Name: \n")
        detailsBuilder.append(fileName)
        detailsBuilder.append("\n\n")

        detailsBuilder.append("File Path: \n")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            detailsBuilder.append(getRelativePath(item.uri, relativePath, fileName))
        } else {
            detailsBuilder.append(relativePath)
        }

        detailsBuilder.append("\n\n")

        detailsBuilder.append("File Size: \n")
        if(size == 0){
            detailsBuilder.append("Loading...")
        } else {
            detailsBuilder.append(
                String.format(
                    "%.2f",
                    (size / (1024f * 1024f))
                )
            )
            detailsBuilder.append(" mb")
        }

        detailsBuilder.append("\n\n")

        detailsBuilder.append("File Created On: \n")
        if(dateAdded == null){
            detailsBuilder.append("Not found")
        } else {
            detailsBuilder.append(dateAdded)
        }

        detailsBuilder.append("\n\n")

        detailsBuilder.append("Last Modified On: \n")
        if(dateModified == null){
            detailsBuilder.append("Not found")
        } else {
            detailsBuilder.append(dateModified)
        }

        alertDialog.setMessage(detailsBuilder)

        alertDialog.setPositiveButton("Close", null)

        alertDialog.show()
    }
*/
    private fun getCurrentItem(): ListItem.MediaItem?  {
        return try {
            (binding.viewPager.adapter as ViewPagerAdapter).currentList[binding.viewPager.currentItem]
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun convertTime(time: Long, showTimeZone: Boolean = true): String {
        val date = Date(time)
        val format = SimpleDateFormat(
            if (showTimeZone) {
                "yyyy-MM-dd HH:mm:ss z"
            } else {
                "yyyy-MM-dd HH:mm:ss"
            }
        )
        format.timeZone = TimeZone.getDefault()
        return format.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    private fun convertTimeForVideo(time: String) : String {
        val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'")
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val parsedDate = dateFormat.parse(time)
        return convertTime(parsedDate?.time ?: 0)
    }

    @SuppressLint("SimpleDateFormat")
    private fun convertTimeForPhoto(time: String, offset: String? = null) : String {

        val timestamp = if (offset != null) {
            "$time $offset"
        } else {
            time
        }

        val dateFormat = SimpleDateFormat(
            if (offset == null) {
                "yyyy:MM:dd HH:mm:ss"
            } else {
                "yyyy:MM:dd HH:mm:ss Z"
            }
        )
        if (offset == null) {
            dateFormat.timeZone = TimeZone.getDefault()
        }
        val parsedDate = dateFormat.parse(timestamp)
        return convertTime(parsedDate?.time ?: 0, offset != null)
    }

    private fun getRelativePath(uri: Uri, path: String?, fileName: String) : String {
        if (path==null) {
            val dPath = URLDecoder.decode(
                uri.lastPathSegment,
                "UTF-8"
            )

            val sType = dPath.substring(0, 7).replaceFirstChar {
                it.uppercase()
            }

            val rPath = dPath.substring(8)

            return "($sType Storage) $rPath"
        }

        return "(Primary Storage) $path$fileName"
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
                        (binding.viewPager.getChildAt(0) as RecyclerView?)?.findViewHolderForLayoutPosition(binding.viewPager.currentItem)
                            as ViewPagerAdapter.ViewHolderPager? ?: return

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