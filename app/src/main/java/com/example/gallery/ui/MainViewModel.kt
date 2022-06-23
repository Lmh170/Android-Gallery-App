package com.example.gallery.ui

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gallery.Album
import com.example.gallery.ListItem
import kotlinx.coroutines.*
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

    private val _binItems = MutableLiveData<List<ListItem.MediaItem>>()
    val binItems: LiveData<List<ListItem.MediaItem>> get() = _binItems

    private var pendingItem: ListItem.MediaItem? = null
    private var pendingItems: List<ListItem.MediaItem>? = null
    private var pendingDescription: String? = null

    private val _permissionNeededForDelete = MutableLiveData<IntentSender>()
    val permissionNeededForDelete: LiveData<IntentSender> = _permissionNeededForDelete

    private val _permissionNeededForEdit = MutableLiveData<IntentSender>()
    val permissionNeededForEdit: LiveData<IntentSender> = _permissionNeededForEdit

    private var contentObserver: ContentObserver? = null

    fun setRecyclerViewItems(items: List<ListItem>) {
        _recyclerViewItems.postValue(items)
        _viewPagerItems.postValue(extractItems(items))
    }

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

    fun loadItems(
        source: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        addSearchWidget: String? = null
    ) {
        viewModelScope.launch {
            val imageList =
                queryItems(source, projection, selection, selectionArgs, addSearchWidget)
            val viewPagerImageList = extractItems(imageList)

            _recyclerViewItems.postValue(imageList)
            _viewPagerItems.postValue(viewPagerImageList)
            _albums.postValue(getAlbums(viewPagerImageList))
        }
    }

    fun uriToMediaItem(contentUri: Uri): ListItem.MediaItem? {

        getApplication<Application>().contentResolver.query(
            convertMediaUriToContentUri(contentUri)!!,
            arrayOf(
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED
            ),
            null,
            null
        )?.use { cursor ->

            if (!cursor.moveToFirst()) return null

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

            return ListItem.MediaItem(
                id,
                uri,
                album,
                type,
                dateModified,
                0,
                0
            )
        }
        return null
    }

    private suspend fun queryItems(): List<ListItem> {
        val items = mutableListOf<ListItem>()

        items += ListItem.Search()

        var listPosition = 0
        var viewPagerPosition = -1

        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED
            )

            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE
                    + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE
                    + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

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
                        items += ListItem.Header(selectedDate.timeInMillis)
                        lastDate = selectedDate
                        listPosition += 1
                    }

                    viewPagerPosition += 1
                    listPosition += 1
                    items += ListItem.MediaItem(
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
        return items
    }

    private suspend fun queryItems(
        source: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        addSearchWidget: String?
    ): List<ListItem> {
        val items = mutableListOf<ListItem>()

        if (addSearchWidget != null) items.add(ListItem.Search(addSearchWidget))

        var listPosition = 0
        var viewPagerPosition = -1

        withContext(Dispatchers.IO) {
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            getApplication<Application>().contentResolver.query(
                source,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                val albumColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val idColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val typeColumn =
                    cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val dateAddedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val album = cursor.getString(albumColumn)
                    val id = cursor.getLong(idColumn)
                    var dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)

                    // convert seconds to milliseconds
                    if (dateAdded < 1000000000000L) dateAdded *= 1000

                    val uri = if (source == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                        )
                    } else if (source == MediaStore.Video.Media.EXTERNAL_CONTENT_URI) {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )
                    } else {
                        if (cursor.getInt(
                                typeColumn
                            )
                            == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        ) {
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                            )
                        } else if (cursor.getInt(
                                typeColumn
                            )
                            == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        ) {
                            ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                            )
                        } else {
                            throw Exception("Unknown type")
                        }
                    }
                    // Todo: merge
                    val type = if (source == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    } else if (source == MediaStore.Video.Media.EXTERNAL_CONTENT_URI) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    } else {
                        if (cursor.getInt(typeColumn)
                            == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        ) {
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        } else {
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        }
                    }

                    viewPagerPosition += 1
                    listPosition += 1

                    items += ListItem.MediaItem(
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
        return items
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

    private fun extractItems(items: List<ListItem>): List<ListItem.MediaItem> {
        val viewPagerImages = mutableListOf<ListItem.MediaItem>()
        items.forEach {
            if (it is ListItem.MediaItem) viewPagerImages.add(it)
        }
        return viewPagerImages
    }

    fun performGetItemInfo(item: ListItem.MediaItem): List<String> {
        val info = mutableListOf<String>()

        val projection =
            arrayOf(
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.Images.Media.DESCRIPTION
            )

        getApplication<Application>().contentResolver.query(
            item.uri,
            projection,
            "${MediaStore.MediaColumns._ID} = ?",
            arrayOf(item.id.toString()),
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
            val description = cursor.getString(4)

            // convert seconds to milliseconds
            if (dateAdded < 1000000000000L) dateAdded *= 1000

            info += dateAdded.toString()
            info += size
            info += path
            info += name
            info += description
        }

        if (item.type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
            val location = getImageLocation(item)
            info += location[0].toString()
            info += location[1].toString()
        }

        return info
    }

    private fun getImageLocation(item: ListItem.MediaItem): DoubleArray {
        val photoUri = MediaStore.setRequireOriginal(item.uri)
        getApplication<Application>().contentResolver.openInputStream(photoUri)?.use { stream ->
            ExifInterface(stream).run {
                return latLong ?: return doubleArrayOf(0.0, 0.0)
            }
        }
        return doubleArrayOf(0.0, 0.0)
    }

    private fun performEditDescription(item: ListItem.MediaItem, description: String) {
        try {
            val photoUri = MediaStore.setRequireOriginal(item.uri)
            getApplication<Application>().contentResolver.openFileDescriptor(photoUri, "w")?.use {
                ExifInterface(it.fileDescriptor).run {
                    setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description)
                    saveAttributes()
                }
            }
        } catch (securityException: SecurityException) {
            val recoverableSecurityException =
                securityException as? RecoverableSecurityException
                    ?: throw securityException

            pendingItem = item
            pendingDescription = description

            _permissionNeededForEdit.postValue(
                recoverableSecurityException.userAction.actionIntent.intentSender
            )
        }
    }

    fun editImageDescription(item: ListItem.MediaItem, description: String) {
        viewModelScope.launch {
            performEditDescription(item, description)
        }
    }

    fun editPendingItemDescription() {
        pendingItem?.let { item ->
            pendingDescription?.let {
                editImageDescription(item, it)
                pendingDescription = null
            }
            pendingItem = null
        }
    }

    fun deletePendingItem() {
        if (pendingItem == null) {
            pendingItems?.let {
                pendingItems = null
                deleteItems(it)
            }
        } else {
            pendingItem?.let {
                pendingItem = null
                deleteItem(it)
            }
        }
    }

    private suspend fun performDeleteItem(image: ListItem.MediaItem, delete: Boolean) {
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createTrashRequest(
                    getApplication<Application>().contentResolver,
                    listOf(image.uri),
                    delete
                )

                pendingItem = null // Item will be deleted with request
                _permissionNeededForDelete.postValue(pendingIntent.intentSender)
                return@withContext
            }
            try {
                getApplication<Application>().contentResolver.delete(
                    image.uri,
                    "${MediaStore.MediaColumns._ID} = ?",
                    arrayOf(image.id.toString())
                )
            } catch (securityException: SecurityException) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                        ?: throw securityException

                // Signal to the Activity that it needs to request permission and
                // try the delete again if it succeeds.
                pendingItem = image
                _permissionNeededForDelete.postValue(
                    recoverableSecurityException.userAction.actionIntent.intentSender
                )
            }
        }
    }

    private suspend fun performDeleteItems(items: List<ListItem.MediaItem>, delete: Boolean) {
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
                        delete
                    )

                    _permissionNeededForDelete.postValue(pendingIntent.intentSender)
                    pendingItems = null // Items will be deleted with request
                } else {
                    items.forEach {
                        getApplication<Application>().contentResolver.delete(
                            it.uri,
                            "${MediaStore.MediaColumns._ID} = ?",
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
                pendingItems = items
                _permissionNeededForDelete.postValue(
                    recoverableSecurityException.userAction.actionIntent.intentSender
                )
            }
        }
    }

    fun deleteItem(image: ListItem.MediaItem, delete: Boolean = true) {
        viewModelScope.launch {
            performDeleteItem(image, delete)
        }
    }

    fun deleteItems(images: List<ListItem.MediaItem>, delete: Boolean = true) {
        viewModelScope.launch {
            performDeleteItems(images, delete)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun loadBin() {
        viewModelScope.launch {
            _binItems.postValue(queryBin())
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun queryBin(): List<ListItem.MediaItem> {
        val images = mutableListOf<ListItem.MediaItem>()

        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )

            val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

            val bundle = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                putString(
                    ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                )
            }

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
        return images
    }

    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }

    /**
     * Convert a media [Uri] to a content [Uri] to be used when requesting
     * [MediaStore.Files.FileColumns] values.
     *
     * Some columns are only available on the [MediaStore.Files] collection and this method converts
     * [Uri] from other MediaStore collections (e.g. [MediaStore.Images])
     *
     * @param uri [Uri] representing the MediaStore entry.
     */
    fun convertMediaUriToContentUri(uri: Uri): Uri? {
        val entryId = uri.lastPathSegment ?: return null

        return MediaStore.Files.getContentUri(MediaStore.getVolumeName(uri), entryId.toLong())
    }
}

/**
 * Convenience extension method to register a [ContentObserver] given a lambda.
 */
fun ContentResolver.registerObserver(
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