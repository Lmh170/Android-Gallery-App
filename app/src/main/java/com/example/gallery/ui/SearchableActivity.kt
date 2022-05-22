package com.example.gallery.ui

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.provider.SearchRecentSuggestions
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                val selection: String
                var selectionArgs: Array<String> = emptyArray()

                if (query.contains("DATE:")) {
                    var extendedQuery = query
                    extendedQuery = extendedQuery.removeRange("DATE:".indices)

                    intent.putExtra(
                        SearchManager.QUERY,
                        SimpleDateFormat.getDateInstance(SimpleDateFormat.FULL).format(
                            Date(extendedQuery.take(10).toLong().times(1000))
                        ) +
                                " - "
                    )

                    selection = "${MediaStore.MediaColumns.DATE_ADDED} >= ?" +
                            " AND " +
                            "${MediaStore.MediaColumns.DATE_ADDED} <= ?"

                    selectionArgs += extendedQuery.take(10)
                    extendedQuery = extendedQuery.removeRange(0..10)

                    intent.putExtra(
                        SearchManager.QUERY,
                        intent.getStringExtra(SearchManager.QUERY) +
                                SimpleDateFormat.getDateInstance(SimpleDateFormat.FULL).format(
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
                    selection = "${MediaStore.Images.Media.DESCRIPTION} LIKE ?"
                    selectionArgs = arrayOf("%$query%")
                }

                viewModel.loadItems(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                        MediaStore.Images.Media.DESCRIPTION
                    ),
                    selection,
                    selectionArgs
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}