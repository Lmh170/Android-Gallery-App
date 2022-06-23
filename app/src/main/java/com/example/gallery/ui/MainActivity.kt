package com.example.gallery.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.gallery.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

open class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    protected val viewModel: MainViewModel by viewModels()
    private lateinit var restoreRequest: ActivityResultLauncher<IntentSenderRequest>

    private val deleteRequest =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.deletePendingItem()
                handleIntent(intent)
            }
        }

    private val editDescriptionRequest =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.editPendingItemDescription()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpMainActivity()
    }

    protected fun setUpMainActivity() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            restoreRequest =
                registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                    if (result.resultCode == RESULT_OK) {
                        viewModel.loadBin()
                    }
                }
        }

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

        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()

        if (!haveStoragePermission(this)) {
            requestStoragePermission(this)
        }
    }

    protected open fun handleIntent(intent: Intent) {
        Log.d(
            this.toString(),
            "intent: action=${intent.action} category=${intent.categories} clipData=${intent.clipData} data=${intent.data} extras=${intent.extras} type=${intent.type}"
        )

        if (intent.action == Intent.ACTION_PICK || intent.action == Intent.ACTION_GET_CONTENT) {
            var source = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            var projection = arrayOf(
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED
            )
            var selection = ""
            var selectionArgs: Array<String>? = null

            when {
                intent.data != null -> {
                    source = viewModel.convertMediaUriToContentUri(intent.data!!)
                    projection += MediaStore.Files.FileColumns.MEDIA_TYPE
                    selection += "(" +
                            MediaStore.Files.FileColumns.MEDIA_TYPE +
                            "=" +
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE +
                            " OR " +
                            MediaStore.Files.FileColumns.MEDIA_TYPE +
                            "=" +
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO +
                            ")"

                }
                intent.type != null -> {
                    if (intent.type!!.contains("image")) {
                        source = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if (intent.type!!.contains("video")) {
                        source = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        projection += MediaStore.Files.FileColumns.MEDIA_TYPE
                        selection += "(" +
                                MediaStore.Files.FileColumns.MEDIA_TYPE +
                                "=" +
                                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE +
                                " OR " +
                                MediaStore.Files.FileColumns.MEDIA_TYPE +
                                "=" +
                                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO +
                                ")"
                    }

                    if (!intent.type!!.contains("*")) {
                        selection += "${MediaStore.MediaColumns.MIME_TYPE} = ?"
                        selectionArgs = arrayOf(intent.type!!)
                    }
                }
                else -> {
                    projection += MediaStore.Files.FileColumns.MEDIA_TYPE
                    selection += "(" +
                            MediaStore.Files.FileColumns.MEDIA_TYPE +
                            "=" +
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE +
                            " OR " +
                            MediaStore.Files.FileColumns.MEDIA_TYPE +
                            "=" +
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO +
                            ")"
                }
            }
            viewModel.loadItems(
                source,
                projection,
                selection,
                selectionArgs
            )
        } else {
            viewModel.loadItems()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handleIntent(intent)
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
                            requestStoragePermission(this)
                        }.show()
                    } else {
                        goToSettings(this)
                    }
                }
            }
        }
    }

    companion object {
        var currentListPosition: Int = 0
        var currentViewPagerPosition: Int = 0
        const val EXTERNAL_STORAGE_REQUEST: Int = 0x1045

        fun haveStoragePermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_MEDIA_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

        fun requestStoragePermission(activity: Activity) {
            if (!haveStoragePermission(activity)) {
                val permissions =
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_MEDIA_LOCATION
                    )
                ActivityCompat.requestPermissions(activity, permissions, EXTERNAL_STORAGE_REQUEST)
            }
        }

        fun goToSettings(activity: Activity) {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${activity.packageName}")
            ).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.also { intent ->
                activity.startActivity(intent)
            }
        }
    }
}