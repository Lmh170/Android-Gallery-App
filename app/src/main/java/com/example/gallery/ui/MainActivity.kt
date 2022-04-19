package com.example.gallery.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    lateinit var restoreRequest: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            restoreRequest =
                registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                    if (it.resultCode == RESULT_OK) {
                        viewModel.loadBin()
                    }
                }
        }

        val deleteRequest =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == RESULT_OK) {
                    viewModel.deletePendingImage()
                    viewModel.loadItems()
                }
            }

        viewModel.permissionNeededForDelete.observe(this) { intentSender ->
            val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
            deleteRequest.launch(intentSenderRequest)
        }

        checkIntent()
    }

    private fun checkIntent() {
        if (intent.action == Intent.ACTION_PICK || intent.action == Intent.ACTION_GET_CONTENT) {
            when {
                intent.data != null -> {
                    viewModel.loadItems(intent.data!!)
                }
                intent.type?.contains("image", true) == true -> {
                    val mimeType: String =
                        intent?.type?.substring(intent.type!!.lastIndexOf("/") + 1)!!
                    if (mimeType != "*" && intent?.type?.contains("/") == false) {
                        viewModel.loadItems(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            mimeType
                        )
                    } else {
                        viewModel.loadItems(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    }
                }
                intent.type?.contains("video", true) == true -> {
                    val mimeType: String =
                        intent?.type?.substring(intent.type!!.lastIndexOf("/") + 1)!!
                    if (mimeType != "*" && intent?.type?.contains("/") == false) {
                        viewModel.loadItems(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            mimeType
                        )
                    } else {
                        viewModel.loadItems(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    }
                }
                else -> viewModel.loadItems()
            }
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
                    viewModel.loadItems()
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
                            requestPermission()
                        }.show()
                    } else {
                        goToSettings()
                    }
                }
            }
        }
    }

    private fun goToSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }

    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            ActivityCompat.requestPermissions(this, permissions, EXTERNAL_STORAGE_REQUEST)
        }
    }

    companion object {
        var currentListPosition = 0
        var currentViewPagerPosition = 0
        lateinit var currentAlbumName: String
        private const val EXTERNAL_STORAGE_REQUEST = 0x1045
    }
}