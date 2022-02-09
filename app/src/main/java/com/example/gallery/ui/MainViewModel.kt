package com.example.gallery.ui

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.IntentSender
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _recyclerViewItems = MutableLiveData<List<ListItem>>()
    val recyclerViewItems: LiveData<List<ListItem>> get() = _recyclerViewItems

    private val _viewPagerImages = MutableLiveData<List<ListItem.MediaItem>>()
    val viewPagerImages: LiveData<List<ListItem.MediaItem>> get() = _viewPagerImages

    private val _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>> get() = _albums

    private var pendingDeleteImage: ListItem.MediaItem? = null
    private var pendingDeleteImages: List<ListItem.MediaItem>? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender>()
    val permissionNeededForDelete: LiveData<IntentSender> = _permissionNeededForDelete

    /**
     * Performs a one shot load of images from [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] into
     * the [_recyclerViewItems] [LiveData] above.
     */
    fun loadItems() {
        viewModelScope.launch {
            val imageList = queryImages()
            val viewPagerImageList = extractItems(imageList)
            _viewPagerImages.postValue(viewPagerImageList)
            _recyclerViewItems.postValue(imageList)
            _albums.postValue(getAlbums(viewPagerImageList))
        }
    }

    fun deleteImage(image: ListItem.MediaItem?) {
        if (image == null) return
        viewModelScope.launch {
            performDeleteImage(image)
        }
    }

    fun deleteImages(images: List<ListItem.MediaItem>) {
        viewModelScope.launch {
            performDeleteImages(images)
        }
    }

    fun deletePendingImage() {
        if (pendingDeleteImage == null) {
            pendingDeleteImages?.let {
                pendingDeleteImages = null
                deleteImages(it)
            }
        } else {
            pendingDeleteImage?.let {
                pendingDeleteImage = null
                deleteImage(it)
            }
        }
    }

    private fun extractItems(items: List<ListItem>): List<ListItem.MediaItem> {
        val viewPagerImages = mutableListOf<ListItem.MediaItem>()
        for (item in items ) {
            if (item is ListItem.MediaItem) viewPagerImages.add(item)
        }
        return viewPagerImages
    }

    private suspend fun queryImages(): List<ListItem> {
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
                val typeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val idColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dateAddedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val buckedDisplayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                var lastDate : Calendar? = null
                while (cursor.moveToNext()) {
                    val type = cursor.getInt(typeColumn)
                    val id = cursor.getLong(idColumn)
                    var dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)

                    // convert seconds to milliseconds
                    if (dateAdded < 1000000000000L) dateAdded *= 1000

                    val album = cursor.getString(buckedDisplayNameColumn)
                    val uri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    }
                    val selectedDate = Calendar.getInstance()
                    selectedDate.timeInMillis = dateAdded
                    if (lastDate == null ||
                        lastDate.get(Calendar.DAY_OF_MONTH) > selectedDate.get(Calendar.DAY_OF_MONTH) ||
                            lastDate.get(Calendar.MONTH) > selectedDate.get(Calendar.MONTH) ||
                            lastDate.get(Calendar.YEAR) > selectedDate.get(Calendar.YEAR))  {
                        selectedDate.set(Calendar.HOUR, 0)
                        selectedDate.set(Calendar.MINUTE, 0)
                        selectedDate.set(Calendar.SECOND, 0)
                        images += ListItem.Header(selectedDate.timeInMillis)
                        lastDate = selectedDate
                        listPosition += 1
                    }
                    viewPagerPosition += 1
                    listPosition += 1
                    images += ListItem.MediaItem(id, uri, album, type, dateModified, viewPagerPosition, listPosition)
                }
            }
        }
        return images
    }

    private suspend fun getAlbums(mediaItems: List<ListItem.MediaItem>?): List<Album> {
        val albums = mutableListOf<Album>()
        /*
        if (mediaItems == null) return albums
        withContext(Dispatchers.Main) {
            albums.add(Album("null", MutableLiveData<List<ListItem.MediaItem>>()))

            for (item in mediaItems) {
                for (i in albums.indices) {
                    if (item.album == albums[i].name){
                        break
                    } else if (i == albums.lastIndex){
                        albums.add(Album(item.album, MutableLiveData<List<ListItem.MediaItem>>()))
                    }
                }
            }
            albums.removeAt(0)

            for (album in albums) {
                val items: MutableList<ListItem.MediaItem> = mutableListOf()
                for (item in mediaItems) {
                    if (item.album == album.name) {
                        items.add(item)
                    }
                }
                album._mediaItems.postValue(items)
            }
        }
       return albums

         */
        mediaItems ?: return albums

        withContext(Dispatchers.Main){
            albums += (Album("null", mutableListOf()))

            for (item in mediaItems) {
                for (i in albums.indices) {
                    if (albums[i].name == item.album) {
                        albums[i].mediaItems += item
                        break
                    } else if (i == albums.size - 1) {
                        albums += Album(item.album, mutableListOf())
                        albums[i+1].mediaItems += item
                    }
                }
            }
            albums.removeAt(0)
        }
        return albums
    }

    private suspend fun performDeleteImage(image: ListItem.MediaItem) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createTrashRequest(getApplication<Application>().contentResolver,
                        listOf(image.uri), true)
                    pendingDeleteImage = null // because the item will be deleted with request == no further action needed
                    _permissionNeededForDelete.postValue(pendingIntent.intentSender)
                } else {
                    getApplication<Application>().contentResolver.delete(
                        image.uri,
                        "${MediaStore.Files.FileColumns._ID} = ?",
                        arrayOf(image.id.toString())
                    )
                }
            } catch (e: SecurityException) {
                pendingDeleteImage = image
                val recoverableSecurityException =
                    e as? RecoverableSecurityException
                        ?: throw e
                val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
                _permissionNeededForDelete.postValue(intentSender)
            }
        }
    }
    private suspend fun performDeleteImages(images: List<ListItem.MediaItem>) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val uris = mutableListOf<Uri>()
                    for (image in images) {
                        uris.add(image.uri)
                    }
                    val pendingIntent = MediaStore.createTrashRequest(getApplication<Application>().contentResolver,
                        uris, true)
                    pendingDeleteImages = null // because the item will be deleted with request == no further action needed
                    _permissionNeededForDelete.postValue(pendingIntent.intentSender)
                } else {
                    var ids = arrayOf<String>()
                    for (image in images) {
                        ids += (image.id.toString())
                    }
                    getApplication<Application>().contentResolver.delete(
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                        "${MediaStore.Files.FileColumns._ID} = ?",
                        ids
                    )
                }
            } catch (e: SecurityException) {
                pendingDeleteImages = images
                val recoverableSecurityException =
                    e as? RecoverableSecurityException
                        ?: throw e
                val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
                _permissionNeededForDelete.postValue(intentSender)
            }
        }
    }
}