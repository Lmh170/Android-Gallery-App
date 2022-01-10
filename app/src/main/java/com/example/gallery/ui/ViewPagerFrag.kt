package com.example.gallery.ui

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.gallery.ListItem
import com.example.gallery.R
import com.example.gallery.adapter.ViewPagerAdapter
import com.example.gallery.databinding.FragmentViewPagerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import java.net.URLDecoder
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
            adapter.submitList(viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems)
       //     adapter.submitList(viewModel.albums.value?.get(MainActivity.currentAlbumPosition)?.mediaItems?.value)
            viewModel.albums.observe(viewLifecycleOwner, {albums ->
                val items = albums.find { it.name == MainActivity.currentAlbumName }?.mediaItems
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
            share.data = currentItem.uri
            share.type = activity?.contentResolver?.getType(currentItem.uri)
            share.putExtra(Intent.EXTRA_STREAM, currentItem.uri)
            share.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(share, "Share with"))
        }
        binding.cvDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialAlertDialog_Centered)
                .setTitle("Permanently delete?")
                .setMessage("This image will be permanently deleted.")
                .setIcon(R.drawable.ic_outline_delete_24)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteImage(getCurrentItem())
                }
                .show()
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
            /*
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

             */
            showCurrentMediaDetails()
        }
    }

    private fun showCurrentMediaDetails() {
        val item = getCurrentItem() ?:return

        val mediaCursor = requireActivity().contentResolver.query(
            item.uri,
            arrayOf(
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE
            ),
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
        detailsBuilder.append(getRelativePath(item.uri, relativePath, fileName))
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


    private fun getCurrentItem():ListItem.MediaItem?  {
        return if (requireArguments().getBoolean("isAlbum")) {
            viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems?.get(binding.viewPager.currentItem)
        } else {
            viewModel.viewPagerImages.value?.get(binding.viewPager.currentItem)
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
            scrimColor = resources.getColor(android.R.color.black, requireActivity().theme)
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