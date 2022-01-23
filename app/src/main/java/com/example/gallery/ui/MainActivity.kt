package com.example.gallery.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.findFragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.MemoryCategory
import com.example.gallery.GlideApp
import com.example.gallery.R
import com.example.gallery.databinding.ActivityMainBinding

private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val request = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == RESULT_OK) {
                viewModel.deletePendingImage()
                viewModel.loadItems()
            }
        }
        viewModel.permissionNeededForDelete.observe(this, { intentSender ->
            val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
            request.launch(intentSenderRequest)
        })

        println("intent: data ${intent.data} type ${intent.type} flags ${intent.flags} action: ${intent.action}" +
                "categories: ${intent.categories} scheme ${intent.scheme}")

        GlideApp.get(this).setMemoryCategory(MemoryCategory.HIGH)
    }

    override fun onStart() {
        super.onStart()
        if (haveStoragePermission()) {
            viewModel.loadItems()
        } else {
            requestPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.loadItems()
                } else {
                    // If we weren't granted the permission, check to see if we should show
                    // rationale for the permission.
                    val showRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )

                    if (showRationale) {
                     //   showNoAccess()
                         // Todo()
                        Toast.makeText(this, "App requires access to storage to access your Photos", Toast.LENGTH_SHORT).show()
                    } else {
                        goToSettings()
                    }
                }
                return
            }
        }
    }

    private fun goToSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).apply {
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
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST)
        }
    }

    companion object {
        var currentListPosition = 0
        var currentViewPagerPosition = 0
        lateinit var currentAlbumName: String
    }
}