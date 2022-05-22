package com.example.gallery.ui

import android.app.SearchManager
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.SearchRecentSuggestions
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.gallery.MySuggestionProvider
import com.example.gallery.databinding.ActivitySearchableBinding
import java.text.SimpleDateFormat
import java.util.*

class SearchableActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivitySearchableBinding
    private lateinit var contentObserver: ContentObserver

    private lateinit var source: Uri
    private lateinit var projection: Array<String>
    private lateinit var selection: String
    private lateinit var selectionArgs: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleIntent(intent)

        val deleteRequest =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    viewModel.deletePendingItem()
                    viewModel.loadItems(
                        source,
                        projection,
                        selection,
                        selectionArgs
                    )
                }
            }

        val editRequest =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    viewModel.editPendingItemDescription()
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
            editRequest.launch(
                IntentSenderRequest.Builder(
                    intentSender
                ).build()
            )
        }
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                if (query.contains("DATE:")) {
                    var extendedQuery = query
                    extendedQuery = extendedQuery.removeRange("DATE:".indices)

                    intent.putExtra(
                        SearchManager.QUERY,
                        SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(
                            Date(extendedQuery.take(10).toLong().times(1000))
                        ) +
                                " - "
                    )

                    source = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

                    projection = arrayOf(
                        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                        MediaStore.Files.FileColumns.MEDIA_TYPE
                    )

                    selection = "(" +
                            MediaStore.Files.FileColumns.MEDIA_TYPE +
                            "=" +
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE +
                            " OR " +
                            MediaStore.Files.FileColumns.MEDIA_TYPE +
                            "=" +
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO +
                            ")" +
                            " AND " +
                            "${MediaStore.MediaColumns.DATE_ADDED} >= ?" +
                            " AND " +
                            "${MediaStore.MediaColumns.DATE_ADDED} <= ?"

                    selectionArgs = arrayOf(extendedQuery.take(10))
                    extendedQuery = extendedQuery.removeRange(0..10)

                    intent.putExtra(
                        SearchManager.QUERY,
                        intent.getStringExtra(SearchManager.QUERY) +
                                SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(
                                    Date(extendedQuery.toLong().times(1000))
                                )
                    )

                    selectionArgs += extendedQuery
                } else {
                    SearchRecentSuggestions(
                        this,
                        MySuggestionProvider.AUTHORITY,
                        MySuggestionProvider.MODE
                    )
                        .saveRecentQuery(query, null)

                    source = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    projection = arrayOf(
                        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                        MediaStore.Images.Media.DESCRIPTION,
                    )
                    selection = "${MediaStore.Images.Media.DESCRIPTION} LIKE ?"
                    selectionArgs = arrayOf("%$query%")
                }

                viewModel.loadItems(
                    source,
                    projection,
                    selection,
                    selectionArgs
                )

                contentObserver = contentResolver.registerObserver(
                    source
                ) {
                    viewModel.loadItems(
                        source,
                        projection,
                        selection,
                        selectionArgs
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun finish() {
        contentResolver.unregisterContentObserver(contentObserver)
        super.finish()
    }
}