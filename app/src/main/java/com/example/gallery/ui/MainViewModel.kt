package com.example.gallery.ui

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gallery.Album
import com.example.gallery.ListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Based on:
 * https://github.com/android/storage-samples/tree/main/MediaStore
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _recyclerViewItems = MutableLiveData<List<ListItem>>()
    val recyclerViewItems: LiveData<List<ListItem>> get() = _recyclerViewItems

    private val _viewPagerItems = MutableLiveData<List<ListItem.MediaItem>>()
    val viewPagerItems: LiveData<List<ListItem.MediaItem>> get() = _viewPagerItems

    private val _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>> get() = _albums

    private val _bin = MutableLiveData<List<ListItem.MediaItem>>()
    val bin: LiveData<List<ListItem.MediaItem>> get() = _bin

    private var pendingDeleteItem: ListItem.MediaItem? = null
    private var pendingDeleteItems: List<ListItem.MediaItem>? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender>()
    val permissionNeededForDelete: LiveData<IntentSender> = _permissionNeededForDelete

    private var contentObserver: ContentObserver? = null

    fun loadItems() {
        viewModelScope.launch {
            val imageList = queryItems()
            val viewPagerImageList = extractItems(imageList)
            _recyclerViewItems.postValue(imageList)
            _viewPagerItems.postValue(viewPagerImageList)
            _albums.postValue(getAlbums(viewPagerImageList))

            if (contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                ) {
                    loadItems()
                }
            }
        }
    }

    fun loadItems(source: Uri, mimeType: String? = null) {
        viewModelScope.launch {
            val imageList = queryItems(source, mimeType)
            val viewPagerImageList = extractItems(imageList)
            _recyclerViewItems.postValue(imageList)
            _viewPagerItems.postValue(viewPagerImageList)
            _albums.postValue(getAlbums(viewPagerImageList))
        }
    }

    fun deletePendingItem() {
        if (pendingDeleteItem == null) {
            pendingDeleteItems?.let {
                pendingDeleteItems = null
                deleteItems(it)
            }
        } else {
            pendingDeleteItem?.let {
                pendingDeleteItem = null
                deleteItem(it)
            }
        }
    }

    fun deleteItem(image: ListItem.MediaItem) {
        viewModelScope.launch {
            performDeleteItem(image)
        }
    }

    fun deleteItems(images: List<ListItem.MediaItem>) {
        viewModelScope.launch {
            performDeleteItems(images)
        }
    }

    private fun extractItems(items: List<ListItem>): List<ListItem.MediaItem> {
        val viewPagerImages = mutableListOf<ListItem.MediaItem>()
        items.forEach {
            if (it is ListItem.MediaItem) viewPagerImages.add(it)
        }
        return viewPagerImages
    }

    private suspend fun queryItems(): List<ListItem> {
        val images = mutableListOf<ListItem>()
        var listPosition = -1 // because the first item has the index 0
        var viewPagerPosition = -1 // because the first item has the index 0

        withContext(Dispatchers.IO) {

            val projection = arrayOf(
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

            getApplication<Application>().contentResolver.query(
                contentUri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->

                var lastDate: Calendar? = null

                while (cursor.moveToNext()) {

                    val album = cursor.getString(0)
                    val id = cursor.getLong(1)
                    val type = cursor.getInt(2)
                    var dateAdded = cursor.getLong(3)
                    val dateModified = cursor.getLong(4)

                    // convert seconds to milliseconds
                    if (dateAdded < 1000000000000L) dateAdded *= 1000

                    val uri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                        )
                    } else {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )
                    }

                    val selectedDate = Calendar.getInstance()
                    selectedDate.timeInMillis = dateAdded

                    // Add header for different days, months etc.
                    if (lastDate == null ||
                        lastDate.get(Calendar.DAY_OF_MONTH) > selectedDate.get(Calendar.DAY_OF_MONTH) ||
                        lastDate.get(Calendar.MONTH) > selectedDate.get(Calendar.MONTH) ||
                        lastDate.get(Calendar.YEAR) > selectedDate.get(Calendar.YEAR)
                    ) {
                        images += ListItem.Header(selectedDate.timeInMillis)
                        lastDate = selectedDate
                        listPosition += 1
                    }

                    viewPagerPosition += 1
                    listPosition += 1
                    images += ListItem.MediaItem(
                        id,
                        uri,
                        album,
                        type,
                        dateModified,
                        viewPagerPosition,
                        listPosition
                    )
                }
            }
        }
        return images
    }

    private suspend fun queryItems(source: Uri, mimeType: String?): List<ListItem> {
        val images = mutableListOf<ListItem>()
        var listPosition = -1 // because the first item has the index 0
        var viewPagerPosition = -1 // because the first item has the index 0

        withContext(Dispatchers.IO) {

            val projection = arrayOf(
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            val selection = if (mimeType != null) {
                MediaStore.Files.FileColumns.MIME_TYPE + " = " + mimeType
            } else {
                null
            }

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            getApplication<Application>().contentResolver.query(
                source,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                var lastDate: Calendar? = null
                while (cursor.moveToNext()) {
                    val album = cursor.getString(0)
                    val id = cursor.getLong(1)
                    var dateAdded = cursor.getLong(2)
                    val dateModified = cursor.getLong(3)

                    // convert seconds to milliseconds
                    if (dateAdded < 1000000000000L) dateAdded *= 1000

                    val uri = if (source == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                        )
                    } else {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )
                    }
                    val type = if (source == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    } else {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    }

                    val selectedDate = Calendar.getInstance()
                    selectedDate.timeInMillis = dateAdded
                    if (lastDate == null ||
                        lastDate.get(Calendar.DAY_OF_MONTH) > selectedDate.get(Calendar.DAY_OF_MONTH) ||
                        lastDate.get(Calendar.MONTH) > selectedDate.get(Calendar.MONTH) ||
                        lastDate.get(Calendar.YEAR) > selectedDate.get(Calendar.YEAR)
                    ) {
                        images += ListItem.Header(selectedDate.timeInMillis)
                        lastDate = selectedDate
                        listPosition += 1
                    }
                    viewPagerPosition += 1
                    listPosition += 1
                    images += ListItem.MediaItem(
                        id,
                        uri,
                        album,
                        type,
                        dateModified,
                        viewPagerPosition,
                        listPosition
                    )
                }
            }
        }
        return images
    }

    private suspend fun getAlbums(mediaItems: List<ListItem.MediaItem>?): List<Album> {
        val albums = mutableListOf<Album>()
        mediaItems ?: return albums

        withContext(Dispatchers.Main) {
            albums += (Album("null", mutableListOf()))

            for (item in mediaItems) {
                for (i in albums.indices) {
                    if (albums[i].name == item.album) {
                        albums[i].mediaItems += item
                        break
                    } else if (i == albums.size - 1) {
                        albums += Album(item.album, mutableListOf())
                        albums[i + 1].mediaItems += item
                    }
                }
            }
            albums.removeAt(0)
        }
        return albums
    }

    fun getItemInfo(source: Uri): List<String> {
        val info = mutableListOf<String>()

        val projection =
            arrayOf(
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
                MediaStore.Files.FileColumns.RELATIVE_PATH,
                MediaStore.Files.FileColumns.DISPLAY_NAME
            )

        getApplication<Application>().contentResolver.query(
            source,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            cursor.moveToFirst()
            var dateAdded = cursor.getLong(0)
            val size = String.format(
                "%.2f",
                (cursor.getLong(1) / (1024f * 1024f))
            )
            val path = cursor.getString(2)
            val name = cursor.getString(3)

            // convert seconds to milliseconds
            if (dateAdded < 1000000000000L) dateAdded *= 1000

            info += dateAdded.toString()
            info += size
            info += path
            info += name
        }
        return info
    }

    private suspend fun performDeleteItem(image: ListItem.MediaItem) {
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createTrashRequest(
                    getApplication<Application>().contentResolver,
                    listOf(image.uri),
                    true
                )

                pendingDeleteItem = null // Item will be deleted with request
                _permissionNeededForDelete.postValue(pendingIntent.intentSender)
                return@withContext
            }
            try {
                getApplication<Application>().contentResolver.delete(
                    image.uri,
                    "${MediaStore.Files.FileColumns._ID} = ?",
                    arrayOf(image.id.toString())
                )
            } catch (securityException: SecurityException) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                        ?: throw securityException

                // Signal to the Activity that it needs to request permission and
                // try the delete again if it succeeds.
                pendingDeleteItem = image
                _permissionNeededForDelete.postValue(
                    recoverableSecurityException.userAction.actionIntent.intentSender
                )
            }
        }
    }

    private suspend fun performDeleteItems(items: List<ListItem.MediaItem>) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val uris = mutableListOf<Uri>()

                    items.forEach {
                        uris.add(it.uri)
                    }

                    val pendingIntent = MediaStore.createTrashRequest(
                        getApplication<Application>().contentResolver,
                        uris,
                        true
                    )

                    _permissionNeededForDelete.postValue(pendingIntent.intentSender)
                    pendingDeleteItems = null // Items will be deleted with request
                } else {
                    items.forEach {
                        val uri = if (it.type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.id
                            )
                        } else {
                            ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, it.id
                            )
                        }

                        getApplication<Application>().contentResolver.delete(
                            uri,
                            "${MediaStore.Files.FileColumns._ID} = ?",
                            arrayOf(it.id.toString())
                        )
                    }
                }
            } catch (securityException: SecurityException) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                        ?: throw securityException

                // Signal to the Activity that it needs to request permission and
                // try the delete again if it succeeds.
                pendingDeleteItems = items
                _permissionNeededForDelete.postValue(
                    recoverableSecurityException.userAction.actionIntent.intentSender
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun queryBin(): List<ListItem.MediaItem> {
        val images = mutableListOf<ListItem.MediaItem>()

        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )

            val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

            val bundle = Bundle()

            bundle.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)

            getApplication<Application>().contentResolver.query(
                contentUri,
                projection,
                bundle,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {

                    val id = cursor.getLong(0)
                    val type = cursor.getInt(1)

                    val uri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                        )
                    } else {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )
                    }

                    images += ListItem.MediaItem(
                        id,
                        uri,
                        "", type,
                        0L,
                        0,
                        0
                    )
                }
            }
        }

        images.reverse()
        return images
    }

    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun loadBin() {
        viewModelScope.launch {
            _bin.postValue(queryBin())
        }
    }
}

/**
 * Convenience extension method to register a [ContentObserver] given a lambda.
 */
private fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler(Looper.myLooper()!!)) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}