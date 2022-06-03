package com.example.gallery.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.example.gallery.ListItem
import com.example.gallery.databinding.ActivityViewPagerBinding
import com.google.android.material.snackbar.Snackbar

class ViewPagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewPagerBinding
    private val viewModel: MainViewModel by viewModels()

    val deleteRequest =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.deletePendingItem()
                handleIntent()
                if (intent.action == Intent.ACTION_VIEW) finish()
            }
        }

    val editDescriptionRequest =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.editPendingItemDescription()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        println("intent: action=${intent.action} category=${intent.categories} clipData=${intent.clipData} data=${intent.data} extras=${intent.extras} type=${intent.type}")

        handleIntent()

        viewModel.permissionNeededForDelete.observe(this) { intentSender ->
            deleteRequest.launch(
                IntentSenderRequest.Builder(
                    intentSender
                ).build()
            )
        }

        viewModel.permissionNeededForEdit.observe(this) { intentSender ->
            editDescriptionRequest.launch(
                IntentSenderRequest.Builder(
                    intentSender
                ).build()
            )
        }
    }

    private fun handleIntent() {
        var source = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        var projection = arrayOf(
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        var selection = ""
        var selectionArgs: Array<String>? = null

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                viewModel.setRecyclerViewItems(
                    listOf(
                        ListItem.MediaItem(
                            Long.MIN_VALUE,
                            intent.data!!,
                            "",
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                            0L,
                            0,
                            0
                        )
                    )
                )
            }
            "com.android.camera.action.REVIEW", MediaStore.ACTION_REVIEW -> {
                if (!MainActivity.haveStoragePermission(this)) {
                    MainActivity.requestStoragePermission(
                        this
                    )
                    return
                }
                projection += MediaStore.Files.FileColumns.MEDIA_TYPE

                selection = "${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} = ?" +
                        " AND " +
                        "(" +
                        MediaStore.Files.FileColumns.MEDIA_TYPE +
                        "=" +
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE +
                        " OR " +
                        MediaStore.Files.FileColumns.MEDIA_TYPE +
                        "=" +
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO +
                        ")"

                selectionArgs = arrayOf(viewModel.uriToMediaItem(intent.data!!)!!.album)

                viewModel.loadItems(
                    source,
                    projection,
                    selection,
                    selectionArgs
                )
            }
            MediaStore.ACTION_REVIEW_SECURE -> {
                // Todo: needs testing
                setShowWhenLocked(true)
                if (!MainActivity.haveStoragePermission(this)) {
                    MainActivity.requestStoragePermission(
                        this
                    )
                    return
                }

                val items = mutableListOf(viewModel.uriToMediaItem(intent.data!!)!!)
                for (i in 0 until intent.clipData?.itemCount!!) {
                    items += viewModel.uriToMediaItem(intent.clipData!!.getItemAt(i).uri)!!
                }
                viewModel.setRecyclerViewItems(items)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MainActivity.EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handleIntent()
                } else {
                    val showRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )

                    if (showRationale) {
                        Snackbar.make(
                            binding.root,
                            "App requires access to storage to access your Photos",
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction("Grant Permission") {
                            MainActivity.requestStoragePermission(this)
                        }.show()
                    } else {
                        MainActivity.goToSettings(this)
                    }
                }
            }
        }
    }
}